package com.wifisense.service;

import com.wifisense.dto.NearbyAccessPoint;
import com.wifisense.dto.NearbyNetworksResponse;
import com.wifisense.dto.WifiScanDetail;
import com.wifisense.dto.WifiScanResponse;
import com.wifisense.model.*;
import com.wifisense.repository.AgentKeyRepository;
import com.wifisense.repository.HomeNetworkRepository;
import com.wifisense.repository.WifiScanObservationRepository;
import com.wifisense.repository.WifiScanSessionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * "Redes cercanas" of Mi Wi-Fi: the user asks the agent of one of their networks to scan, follows the scan and
 * reads what it saw and the history. Every method first checks that the network belongs to the signed-in user, so
 * nobody can read or start the scans of another account (the answer is the same "not found" as for a network that
 * does not exist).
 */
@Service
public class WifiScanService {

    static final int MAX_HISTORY = 50;

    private final HomeNetworkRepository networks;
    private final AgentKeyRepository keys;
    private final WifiScanSessionRepository scans;
    private final WifiScanObservationRepository observations;
    private final WifiScanQueue queue;
    private final WifiScanProperties properties;
    private final AgentProperties agent;
    private final Clock clock;

    public WifiScanService(HomeNetworkRepository networks, AgentKeyRepository keys, WifiScanSessionRepository scans,
                           WifiScanObservationRepository observations, WifiScanQueue queue,
                           WifiScanProperties properties, AgentProperties agent, Clock clock) {
        this.networks = networks;
        this.keys = keys;
        this.scans = scans;
        this.observations = observations;
        this.queue = queue;
        this.properties = properties;
        this.agent = agent;
        this.clock = clock;
    }

    /**
     * "Escanear ahora". Refused (with the reason) when the network has no agent, when the agent has not been seen
     * lately (nobody would run the scan), when a scan is already waiting or running, or right after another one.
     */
    @Transactional
    public WifiScanResponse request(String username, Long networkId) {
        HomeNetwork network = own(username, networkId);
        Instant now = Instant.now(clock);
        AgentKey key = keys.findByNetworkId(networkId)
                .orElseThrow(() -> new IllegalStateException("Esta red aún no tiene el agente instalado."));
        if (!online(key, now)) {
            throw new IllegalStateException("El agente de esta red no se ha conectado en los últimos "
                    + agent.onlineWindow().toMinutes() + " minutos: abre WiFiSense Agente en el computador y vuelve a "
                    + "intentar.");
        }
        Instant availableAt = manualAvailableAt(networkId);
        if (availableAt != null && availableAt.isAfter(now)) {
            throw new ScanLimitException("Acabas de escanear. Espera unos segundos para volver a hacerlo.",
                    Duration.between(now, availableAt).toSeconds() + 1);
        }
        return WifiScanResponse.from(queue.enqueue(networkId, ScanOrigin.MANUAL, network.getOwner().getId(), now));
    }

    /** The panel: newest scan (any state), last completed scan with what it saw, and whether that is out of date. */
    @Transactional
    public NearbyNetworksResponse nearby(String username, Long networkId) {
        own(username, networkId);
        Instant now = Instant.now(clock);
        Optional<AgentKey> key = keys.findByNetworkId(networkId);
        queue.active(networkId, now);  // a scan the agent abandoned shows as failed, not as "escaneando" forever
        Optional<WifiScanSession> current = scans.findFirstByNetworkIdOrderByRequestedAtDesc(networkId);
        Optional<WifiScanSession> completed =
                scans.findFirstByNetworkIdAndStatusOrderByFinishedAtDesc(networkId, ScanStatus.COMPLETED);
        boolean stale = completed.map(scan -> !scan.getFinishedAt().plus(properties.staleAfter()).isAfter(now))
                .orElse(false);
        List<NearbyAccessPoint> seen = completed.map(scan -> observations.findByScan(scan.getId()).stream()
                .map(NearbyAccessPoint::from).toList()).orElse(List.of());
        return new NearbyNetworksResponse(key.isPresent(), key.map(k -> online(k, now)).orElse(false),
                current.map(WifiScanResponse::from).orElse(null), completed.map(WifiScanResponse::from).orElse(null),
                stale, properties.staleAfter().toMinutes(),
                key.isPresent() ? queue.nextScheduled(networkId, now) : null, manualAvailableAt(networkId), seen);
    }

    /** The scans of a network, newest first (at most {@value #MAX_HISTORY}). */
    @Transactional
    public List<WifiScanResponse> history(String username, Long networkId, int limit) {
        own(username, networkId);
        queue.active(networkId, Instant.now(clock));
        return scans.findByNetworkIdOrderByRequestedAtDesc(networkId,
                        PageRequest.of(0, Math.min(Math.max(limit, 1), MAX_HISTORY)))
                .stream().map(WifiScanResponse::from).toList();
    }

    /** One scan of the history and what it saw. */
    @Transactional(readOnly = true)
    public WifiScanDetail detail(String username, Long networkId, Long scanId) {
        own(username, networkId);
        WifiScanSession scan = scans.findByIdAndNetworkId(scanId, networkId)
                .orElseThrow(() -> new ResourceNotFoundException("el escaneo", scanId));
        return new WifiScanDetail(WifiScanResponse.from(scan),
                observations.findByScan(scanId).stream().map(NearbyAccessPoint::from).toList());
    }

    /** When "Escanear ahora" can be pressed again, or null if it has never been pressed. */
    private Instant manualAvailableAt(Long networkId) {
        return scans.findFirstByNetworkIdAndOriginOrderByRequestedAtDesc(networkId, ScanOrigin.MANUAL)
                .map(last -> last.getRequestedAt().plus(properties.manualMinInterval())).orElse(null);
    }

    private boolean online(AgentKey key, Instant now) {
        return key.getLastSeenAt() != null && key.getLastSeenAt().plus(agent.onlineWindow()).isAfter(now);
    }

    private HomeNetwork own(String username, Long networkId) {
        return networks.findByIdAndOwner_Username(networkId, username)
                .orElseThrow(() -> new ResourceNotFoundException("la red", networkId));
    }
}
