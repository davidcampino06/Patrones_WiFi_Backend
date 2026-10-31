package com.wifisense.service;

import com.wifisense.model.*;
import com.wifisense.repository.HomeNetworkRepository;
import com.wifisense.repository.WifiScanSessionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * The scans waiting for the agent of each network. Shared by the page (which adds manual scans) and by the agent
 * (which picks them up): a network has at most one scan waiting or running, a scan the agent does not finish in
 * time fails instead of waiting forever, and the schedule adds a scan when the last one is old.
 * Every method must run inside a transaction.
 */
@Component
public class WifiScanQueue {

    static final Set<ScanStatus> ACTIVE = EnumSet.of(ScanStatus.PENDING, ScanStatus.RUNNING);

    private final WifiScanSessionRepository scans;
    private final HomeNetworkRepository networks;
    private final WifiScanProperties properties;

    public WifiScanQueue(WifiScanSessionRepository scans, HomeNetworkRepository networks,
                         WifiScanProperties properties) {
        this.scans = scans;
        this.networks = networks;
        this.properties = properties;
    }

    /** The scan waiting or running on the network; one the agent left unfinished for too long fails first. */
    public Optional<WifiScanSession> active(Long networkId, Instant now) {
        Optional<WifiScanSession> active = scans.findFirstByNetworkIdAndStatusIn(networkId, ACTIVE);
        if (active.isPresent() && expired(active.get(), now)) {
            active.get().fail(now, ScanFailure.AGENT_NO_RESPONSE, null);
            scans.saveAndFlush(active.get());
            return Optional.empty();
        }
        return active;
    }

    boolean expired(WifiScanSession scan, Instant now) {
        Instant since = scan.getStartedAt() != null ? scan.getStartedAt() : scan.getRequestedAt();
        return scan.getStatus().active() && !since.plus(properties.agentTimeout()).isAfter(now);
    }

    /** Adds a scan for the user; fails if one is already waiting or running. */
    public WifiScanSession enqueue(Long networkId, ScanOrigin origin, Long requestedBy, Instant now) {
        if (active(networkId, now).isPresent()) {
            throw new IllegalStateException("Ya hay un escaneo en curso en esta red.");
        }
        return scans.saveAndFlush(new WifiScanSession(networks.getReferenceById(networkId), origin, requestedBy, now));
    }

    /** When the schedule will ask for the next scan: the last scan (of any kind) plus the interval. */
    public Instant nextScheduled(Long networkId, Instant now) {
        return scans.findFirstByNetworkIdOrderByRequestedAtDesc(networkId)
                .map(last -> last.getRequestedAt().plus(properties.scheduledInterval()))
                .orElse(now);
    }

    /**
     * The scan the agent must run now: the one waiting (which becomes RUNNING), the one it already picked up (it may
     * have restarted), or a scheduled one when the last scan is old. Empty when there is nothing to do.
     */
    public Optional<WifiScanSession> take(Long networkId, Instant now, boolean schedule) {
        Optional<WifiScanSession> active = active(networkId, now);
        if (active.isEmpty() && schedule && !nextScheduled(networkId, now).isAfter(now)) {
            active = Optional.of(enqueue(networkId, ScanOrigin.SCHEDULED, null, now));
        }
        active.filter(scan -> scan.getStatus() == ScanStatus.PENDING).ifPresent(scan -> {
            scan.start(now);
            scans.saveAndFlush(scan);
        });
        return active;
    }
}
