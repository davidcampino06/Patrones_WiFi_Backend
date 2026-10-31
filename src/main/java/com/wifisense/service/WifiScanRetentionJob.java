package com.wifisense.service;

import com.wifisense.repository.WifiAccessPointRepository;
import com.wifisense.repository.WifiScanSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Every night deletes the scans older than {@code wifisense.scan.retention} (30 days; their observations go with
 * them) and the access points not seen for {@code wifisense.scan.access-point-retention} (90 days). A scheduled scan
 * every hour leaves about 24 scans a day per network: keeping them forever would only grow the database with data
 * about the user's surroundings.
 */
@Component
public class WifiScanRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(WifiScanRetentionJob.class);

    private final WifiScanSessionRepository scans;
    private final WifiAccessPointRepository accessPoints;
    private final WifiScanProperties properties;
    private final Clock clock;

    public WifiScanRetentionJob(WifiScanSessionRepository scans, WifiAccessPointRepository accessPoints,
                                WifiScanProperties properties, Clock clock) {
        this.scans = scans;
        this.accessPoints = accessPoints;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${wifisense.scan.cleanup-cron:0 45 3 * * *}", zone = "UTC")
    @Transactional
    public void purge() {
        Instant now = Instant.now(clock);
        int deletedScans = scans.deleteOlderThan(now.minus(properties.retention()));
        int deletedAccessPoints = accessPoints.deleteNotSeenSince(now.minus(properties.accessPointRetention()));
        if (deletedScans + deletedAccessPoints > 0) {
            log.info("Scan retention: deleted {} scans and {} access points", deletedScans, deletedAccessPoints);
        }
    }
}
