package com.wifisense.service;

import com.wifisense.dto.AgentScanResult;
import com.wifisense.model.*;
import com.wifisense.repository.WifiScanSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentScanServiceTest {

    private static final String KEY = "wsa_" + "k".repeat(43);
    private static final Instant NOW = Instant.parse("2026-10-26T20:00:00Z");

    private final AgentKeyService keyService = mock(AgentKeyService.class);
    private final WifiScanSessionRepository scans = mock(WifiScanSessionRepository.class);
    private final WifiScanQueue queue = mock(WifiScanQueue.class);
    private final WifiScanRecorder recorder = mock(WifiScanRecorder.class);
    private final MovableClock clock = new MovableClock();
    private final AgentScanService service = new AgentScanService(keyService, scans, queue, recorder,
            WifiScanQueueTest.PROPERTIES, clock);
    private final HomeNetwork casa = mock(HomeNetwork.class);
    private final WifiScanSession running = new WifiScanSession(casa, ScanOrigin.MANUAL, 3L, NOW.minusSeconds(30));

    @BeforeEach
    void setUp() {
        AgentKey key = new AgentKey(casa, "a".repeat(64), "wsa_kkkk");
        ReflectionTestUtils.setField(key, "id", 5L);
        when(casa.getId()).thenReturn(7L);
        when(keyService.authenticate(KEY)).thenReturn(key);
        ReflectionTestUtils.setField(running, "id", 50L);
        running.start(NOW.minusSeconds(20));
        when(scans.findByIdAndNetworkId(50L, 7L)).thenReturn(Optional.of(running));
        when(scans.save(any(WifiScanSession.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static AgentScanResult completed(List<AgentScanResult.AccessPoint> seen) {
        return new AgentScanResult(50L, "COMPLETED", null, "Wi-Fi", "NETSH", true, 4100, "1.9.0", seen);
    }

    @Test
    void theAgentIsToldWhichScanToRunAndNothingElse() {
        when(queue.take(7L, NOW, false)).thenReturn(Optional.of(running));

        assertThat(service.next(KEY)).hasValueSatisfying(task -> assertThat(task.scanId()).isEqualTo(50L));
    }

    @Test
    void anAgentThatAsksTooOftenIsToldToWait() {
        when(queue.take(anyLong(), any(), anyBoolean())).thenReturn(Optional.empty());
        service.next(KEY);

        clock.advance(Duration.ofSeconds(4));
        assertThatThrownBy(() -> service.next(KEY)).isInstanceOf(ScanLimitException.class)
                .satisfies(e -> assertThat(((ScanLimitException) e).getRetryAfterSeconds()).isEqualTo(7));
        clock.advance(Duration.ofSeconds(6));
        assertThat(service.next(KEY)).isEmpty();
    }

    @Test
    void anInvalidKeyNeverReachesTheScans() {
        when(keyService.authenticate("wsa_falsa")).thenThrow(new InvalidAgentKeyException());

        assertThatThrownBy(() -> service.next("wsa_falsa")).isInstanceOf(InvalidAgentKeyException.class);
        assertThatThrownBy(() -> service.receive("wsa_falsa", completed(List.of())))
                .isInstanceOf(InvalidAgentKeyException.class);
        verifyNoInteractions(queue, scans, recorder);
    }

    @Test
    void theReportOfTheAgentCarriesTheScanDueBySchedule() {
        when(queue.take(7L, NOW, true)).thenReturn(Optional.of(running));

        assertThat(service.dueWithReport(KEY)).contains(50L);
    }

    @Test
    void aCompletedScanStoresWhatItSaw() {
        List<AgentScanResult.AccessPoint> seen = List.of(WifiScanRecorderTest.ap("a".repeat(24), "Casa", 80));
        when(recorder.record(running, seen, NOW)).thenReturn(new WifiScanRecorder.Stored(1, 1));

        WifiScanSession saved = service.receive(KEY, completed(seen));

        assertThat(saved.getStatus()).isEqualTo(ScanStatus.COMPLETED);
        assertThat(saved.getAccessPointCount()).isEqualTo(1);
        assertThat(saved.getFresh()).isTrue();
        assertThat(saved.getDurationMs()).isEqualTo(4100);
    }

    @Test
    void aFailedScanKeepsTheReasonTheAgentFound() {
        WifiScanSession saved = service.receive(KEY, new AgentScanResult(50L, "FAILED", "ADAPTER_OFF", "Wi-Fi",
                "NETSH", false, 300, "1.9.0", null));

        assertThat(saved.getStatus()).isEqualTo(ScanStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ScanFailure.ADAPTER_OFF);
        verifyNoInteractions(recorder);
    }

    @Test
    void aFailureWithoutReasonIsACommandThatFailed() {
        WifiScanSession saved = service.receive(KEY, new AgentScanResult(50L, "FAILED", null, null, null, null, null,
                "1.9.0", null));

        assertThat(saved.getFailureReason()).isEqualTo(ScanFailure.COMMAND_FAILED);
    }

    @Test
    void anAgentCannotDeliverToAScanOfAnotherNetwork() {
        when(scans.findByIdAndNetworkId(77L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.receive(KEY, new AgentScanResult(77L, "COMPLETED", null, null, null, null,
                null, "1.9.0", List.of()))).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(recorder);
    }

    @Test
    void aResultThatArrivesAfterTheTimeLimitIsRefused() {
        when(queue.expired(running, NOW)).thenReturn(true);

        assertThatThrownBy(() -> service.receive(KEY, completed(List.of())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("llegó tarde");
        verifyNoInteractions(recorder);
    }

    @Test
    void aScanCannotBeDeliveredTwice() {
        when(recorder.record(any(), any(), any())).thenReturn(new WifiScanRecorder.Stored(0, 0));
        service.receive(KEY, completed(List.of()));

        assertThatThrownBy(() -> service.receive(KEY, completed(List.of())))
                .isInstanceOf(IllegalStateException.class).hasMessage("Ese escaneo ya terminó.");
        verify(recorder, times(1)).record(any(), any(), any());  // the second copy stores nothing
    }

    /** A clock the test moves forward. */
    private static final class MovableClock extends Clock {
        private Instant now = NOW;

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
