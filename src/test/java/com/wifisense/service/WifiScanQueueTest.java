package com.wifisense.service;

import com.wifisense.model.*;
import com.wifisense.repository.HomeNetworkRepository;
import com.wifisense.repository.WifiScanSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WifiScanQueueTest {

    static final WifiScanProperties PROPERTIES = new WifiScanProperties(Duration.ofMinutes(60), Duration.ofSeconds(30),
            Duration.ofMinutes(3), Duration.ofSeconds(10), Duration.ofMinutes(150), 200, Duration.ofDays(30),
            Duration.ofDays(90));
    private static final Instant NOW = Instant.parse("2026-10-26T20:00:00Z");

    private final WifiScanSessionRepository scans = mock(WifiScanSessionRepository.class);
    private final HomeNetworkRepository networks = mock(HomeNetworkRepository.class);
    private final WifiScanQueue queue = new WifiScanQueue(scans, networks, PROPERTIES);
    private final HomeNetwork casa = mock(HomeNetwork.class);

    @BeforeEach
    void setUp() {
        when(networks.getReferenceById(7L)).thenReturn(casa);
        when(scans.saveAndFlush(any(WifiScanSession.class))).thenAnswer(call -> call.getArgument(0));
    }

    private WifiScanSession scan(ScanOrigin origin, Instant requested) {
        WifiScanSession scan = new WifiScanSession(casa, origin, null, requested);
        ReflectionTestUtils.setField(scan, "id", 40L);
        return scan;
    }

    @Test
    void aScanTheAgentAbandonedFailsInsteadOfWaitingForever() {
        WifiScanSession old = scan(ScanOrigin.MANUAL, NOW.minus(Duration.ofMinutes(3)));
        when(scans.findFirstByNetworkIdAndStatusIn(eq(7L), any())).thenReturn(Optional.of(old));

        assertThat(queue.active(7L, NOW)).isEmpty();
        assertThat(old.getStatus()).isEqualTo(ScanStatus.FAILED);
        assertThat(old.getFailureReason()).isEqualTo(ScanFailure.AGENT_NO_RESPONSE);
        verify(scans).saveAndFlush(old);
    }

    @Test
    void aRecentScanStaysActive() {
        WifiScanSession recent = scan(ScanOrigin.MANUAL, NOW.minusSeconds(40));
        when(scans.findFirstByNetworkIdAndStatusIn(eq(7L), any())).thenReturn(Optional.of(recent));

        assertThat(queue.active(7L, NOW)).contains(recent);
        assertThat(recent.getStatus()).isEqualTo(ScanStatus.PENDING);
    }

    @Test
    void aRunningScanCountsItsTimeFromWhenTheAgentPickedItUp() {
        WifiScanSession running = scan(ScanOrigin.MANUAL, NOW.minus(Duration.ofMinutes(10)));
        running.start(NOW.minusSeconds(60));

        assertThat(queue.expired(running, NOW)).isFalse();
        assertThat(queue.expired(running, NOW.plus(Duration.ofMinutes(2)))).isTrue();
    }

    @Test
    void onlyOneScanWaitsPerNetwork() {
        when(scans.findFirstByNetworkIdAndStatusIn(eq(7L), any()))
                .thenReturn(Optional.of(scan(ScanOrigin.SCHEDULED, NOW.minusSeconds(5))));

        assertThatThrownBy(() -> queue.enqueue(7L, ScanOrigin.MANUAL, 3L, NOW))
                .isInstanceOf(IllegalStateException.class).hasMessage("Ya hay un escaneo en curso en esta red.");
        verify(scans, never()).saveAndFlush(any());
    }

    @Test
    void theAgentPicksUpTheWaitingScanAndItStartsRunning() {
        WifiScanSession waiting = scan(ScanOrigin.MANUAL, NOW.minusSeconds(20));
        when(scans.findFirstByNetworkIdAndStatusIn(eq(7L), any())).thenReturn(Optional.of(waiting));

        assertThat(queue.take(7L, NOW, false)).contains(waiting);
        assertThat(waiting.getStatus()).isEqualTo(ScanStatus.RUNNING);
        assertThat(waiting.getStartedAt()).isEqualTo(NOW);
    }

    @Test
    void theScheduleAddsAScanWhenTheLastOneIsOld() {
        when(scans.findFirstByNetworkIdAndStatusIn(eq(7L), any())).thenReturn(Optional.empty());
        when(scans.findFirstByNetworkIdOrderByRequestedAtDesc(7L))
                .thenReturn(Optional.of(scan(ScanOrigin.SCHEDULED, NOW.minus(Duration.ofMinutes(61)))));

        Optional<WifiScanSession> taken = queue.take(7L, NOW, true);

        assertThat(taken).isPresent();
        assertThat(taken.get().getOrigin()).isEqualTo(ScanOrigin.SCHEDULED);
        assertThat(taken.get().getStatus()).isEqualTo(ScanStatus.RUNNING);
        assertThat(taken.get().getRequestedBy()).isNull();
    }

    @Test
    void theScheduleWaitsWhileTheLastScanIsRecent() {
        when(scans.findFirstByNetworkIdAndStatusIn(eq(7L), any())).thenReturn(Optional.empty());
        when(scans.findFirstByNetworkIdOrderByRequestedAtDesc(7L))
                .thenReturn(Optional.of(scan(ScanOrigin.MANUAL, NOW.minus(Duration.ofMinutes(20)))));

        assertThat(queue.take(7L, NOW, true)).isEmpty();
        assertThat(queue.nextScheduled(7L, NOW)).isEqualTo(NOW.plus(Duration.ofMinutes(40)));
        verify(scans, never()).saveAndFlush(any());
    }

    @Test
    void aNetworkThatNeverScannedIsDueNow() {
        when(scans.findFirstByNetworkIdOrderByRequestedAtDesc(7L)).thenReturn(Optional.empty());

        assertThat(queue.nextScheduled(7L, NOW)).isEqualTo(NOW);
    }

    @Test
    void askingBetweenReportsNeverStartsAScheduledScan() {
        when(scans.findFirstByNetworkIdAndStatusIn(eq(7L), any())).thenReturn(Optional.empty());
        when(scans.findFirstByNetworkIdOrderByRequestedAtDesc(7L)).thenReturn(Optional.empty());

        assertThat(queue.take(7L, NOW, false)).isEmpty();
        verify(scans, never()).saveAndFlush(any());
    }
}
