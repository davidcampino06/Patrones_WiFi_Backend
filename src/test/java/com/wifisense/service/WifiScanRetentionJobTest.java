package com.wifisense.service;

import com.wifisense.repository.WifiAccessPointRepository;
import com.wifisense.repository.WifiScanSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.*;

class WifiScanRetentionJobTest {

    @Test
    void deletesScansAfterThirtyDaysAndAccessPointsNotSeenForNinety() {
        Instant now = Instant.parse("2026-10-26T03:45:00Z");
        WifiScanSessionRepository scans = mock(WifiScanSessionRepository.class);
        WifiAccessPointRepository accessPoints = mock(WifiAccessPointRepository.class);

        new WifiScanRetentionJob(scans, accessPoints, WifiScanQueueTest.PROPERTIES, Clock.fixed(now, ZoneOffset.UTC))
                .purge();

        verify(scans).deleteOlderThan(Instant.parse("2026-09-26T03:45:00Z"));
        verify(accessPoints).deleteNotSeenSince(Instant.parse("2026-07-28T03:45:00Z"));
    }
}
