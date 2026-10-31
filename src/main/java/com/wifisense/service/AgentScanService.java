package com.wifisense.service;

import com.wifisense.dto.AgentScanResult;
import com.wifisense.dto.AgentScanTask;
import com.wifisense.model.AgentKey;
import com.wifisense.model.ScanFailure;
import com.wifisense.model.WifiScanSession;
import com.wifisense.repository.WifiScanSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The agent's side of the scans. The agent authenticates with its key (the key decides the network: the request
 * never says it), asks every 30 seconds whether there is a scan to run, and delivers each result to the scan it was
 * given. The server never sends the agent a command: only the number of a scan to run.
 */
@Service
public class AgentScanService {

    private static final Logger log = LoggerFactory.getLogger(AgentScanService.class);

    private final AgentKeyService keyService;
    private final WifiScanSessionRepository scans;
    private final WifiScanQueue queue;
    private final WifiScanRecorder recorder;
    private final WifiScanProperties properties;
    private final Clock clock;
    private final Map<Long, Instant> lastPoll = new ConcurrentHashMap<>();

    public AgentScanService(AgentKeyService keyService, WifiScanSessionRepository scans, WifiScanQueue queue,
                            WifiScanRecorder recorder, WifiScanProperties properties, Clock clock) {
        this.keyService = keyService;
        this.scans = scans;
        this.queue = queue;
        this.recorder = recorder;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Called after each accepted report of an agent of Mi Wi-Fi: the scan to run now, if any (asked from the page, or
     * added by the schedule when the last scan is old).
     */
    @Transactional
    public Optional<Long> dueWithReport(String rawKey) {
        AgentKey key = keyService.authenticate(rawKey);
        return queue.take(key.getNetwork().getId(), Instant.now(clock), true).map(WifiScanSession::getId);
    }

    /** "Is there a scan for me?" between reports. Only manual scans are handed out here; the schedule waits. */
    @Transactional
    public Optional<AgentScanTask> next(String rawKey) {
        AgentKey key = keyService.authenticate(rawKey);
        Instant now = Instant.now(clock);
        lastPoll.compute(key.getId(), (id, previous) -> {
            if (previous != null && previous.plus(properties.pollMinInterval()).isAfter(now)) {
                throw new ScanLimitException("El agente preguntó por escaneos demasiado seguido.",
                        Duration.between(now, previous.plus(properties.pollMinInterval())).toSeconds() + 1);
            }
            return now;
        });
        return queue.take(key.getNetwork().getId(), now, false).map(scan -> new AgentScanTask(scan.getId()));
    }

    /**
     * The result of a scan. It must be a scan of the agent's own network that is still waiting or running: a scan of
     * another network is "not found" (the same answer as one that does not exist), and a finished one is refused.
     */
    @Transactional
    public WifiScanSession receive(String rawKey, AgentScanResult result) {
        AgentKey key = keyService.authenticate(rawKey);
        Instant now = Instant.now(clock);
        Long networkId = key.getNetwork().getId();
        WifiScanSession scan = scans.findByIdAndNetworkId(result.scanId(), networkId)
                .orElseThrow(() -> new ResourceNotFoundException("el escaneo", result.scanId()));
        if (!scan.getStatus().active()) {
            throw new IllegalStateException("Ese escaneo ya terminó.");
        }
        if (queue.expired(scan, now)) {
            throw new IllegalStateException("Ese escaneo ya terminó: el resultado llegó tarde.");
        }
        WifiScanSession.ScanDetails details = new WifiScanSession.ScanDetails(
                SsidText.cleanLabel(result.interfaceName(), 64), result.scanTool(), result.fresh(),
                result.durationMs(), result.agentVersion());
        if ("FAILED".equals(result.status())) {
            scan.fail(now, result.failureReason() == null ? ScanFailure.COMMAND_FAILED
                    : ScanFailure.valueOf(result.failureReason()), details);
        } else {
            WifiScanRecorder.Stored stored = recorder.record(scan, result.accessPointsOrEmpty(), now);
            scan.complete(now, details, stored.networks(), stored.accessPoints());
        }
        log.info("Scan {} of network {}: {} ({} access points)", scan.getId(), networkId, scan.getStatus(),
                scan.getAccessPointCount());
        return scans.save(scan);
    }
}
