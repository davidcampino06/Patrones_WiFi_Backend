package com.wifisense.service;

import com.wifisense.dto.AgentScanResult;
import com.wifisense.model.WifiAccessPoint;
import com.wifisense.model.WifiScanObservation;
import com.wifisense.model.WifiScanSession;
import com.wifisense.repository.WifiAccessPointRepository;
import com.wifisense.repository.WifiScanObservationRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stores what a completed scan saw. Each access point is kept once per network (found by the fingerprint of its
 * BSSID), so repeating a scan updates it instead of copying it; the scan adds one observation per access point.
 * Must run inside a transaction.
 */
@Component
public class WifiScanRecorder {

    private static final Comparator<AgentScanResult.AccessPoint> STRONGEST_FIRST = Comparator.comparing(
            (AgentScanResult.AccessPoint ap) -> ap.signalPercent() == null ? -1 : ap.signalPercent()).reversed();

    private final WifiAccessPointRepository accessPoints;
    private final WifiScanObservationRepository observations;
    private final WifiScanProperties properties;

    public WifiScanRecorder(WifiAccessPointRepository accessPoints, WifiScanObservationRepository observations,
                            WifiScanProperties properties) {
        this.accessPoints = accessPoints;
        this.observations = observations;
        this.properties = properties;
    }

    /** What is stored of a scan: how many distinct networks and how many access points. */
    public record Stored(int networks, int accessPoints) {
    }

    public Stored record(WifiScanSession scan, List<AgentScanResult.AccessPoint> seen, Instant now) {
        List<AgentScanResult.AccessPoint> kept = keep(seen, properties.maxAccessPoints());
        Long networkId = scan.getNetwork().getId();
        Map<String, WifiAccessPoint> known = accessPoints.findByNetworkIdAndBssidFingerprintIn(networkId,
                kept.stream().map(AgentScanResult.AccessPoint::bssid).toList()).stream()
                .collect(Collectors.toMap(WifiAccessPoint::getBssidFingerprint, Function.identity()));
        List<WifiScanObservation> rows = new ArrayList<>();
        for (AgentScanResult.AccessPoint ap : kept) {
            String ssid = SsidText.clean(ap.ssid());
            WifiAccessPoint stored = known.computeIfAbsent(ap.bssid(),
                    fingerprint -> new WifiAccessPoint(scan.getNetwork(), fingerprint, now));
            stored.seen(ssid, SsidText.cleanLabel(ap.vendor(), 60), ap.bssidSuffix(), now);
            rows.add(new WifiScanObservation(scan, accessPoints.save(stored), new WifiScanObservation.Radio(ssid,
                    ap.signalPercent(), ap.signalDbm(), ap.channel(), ap.band(),
                    SsidText.cleanLabel(ap.radioType(), 20), ap.authentication(),
                    SsidText.cleanLabel(ap.securityLabel(), 40), SsidText.cleanLabel(ap.cipher(), 20),
                    Boolean.TRUE.equals(ap.connected()))));
        }
        observations.saveAll(rows);
        return new Stored(distinctNetworks(kept), kept.size());
    }

    /**
     * One entry per BSSID (an agent should not repeat one, but if it does the strongest reading wins) and at most
     * {@code limit}, the strongest ones.
     */
    static List<AgentScanResult.AccessPoint> keep(List<AgentScanResult.AccessPoint> seen, int limit) {
        Map<String, AgentScanResult.AccessPoint> unique = new LinkedHashMap<>();
        seen.stream().sorted(STRONGEST_FIRST).forEach(ap -> unique.putIfAbsent(ap.bssid(), ap));
        return unique.values().stream().limit(Math.max(limit, 0)).toList();
    }

    /** Networks seen: distinct names, and each hidden access point counts as one (it may be any network). */
    static int distinctNetworks(List<AgentScanResult.AccessPoint> kept) {
        Set<String> names = new HashSet<>();
        int hidden = 0;
        for (AgentScanResult.AccessPoint ap : kept) {
            String ssid = SsidText.clean(ap.ssid());
            if (ssid == null) {
                hidden++;
            } else {
                names.add(ssid);
            }
        }
        return names.size() + hidden;
    }
}
