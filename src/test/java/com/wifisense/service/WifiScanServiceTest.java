package com.wifisense.service;

import com.wifisense.dto.NearbyNetworksResponse;
import com.wifisense.dto.WifiScanResponse;
import com.wifisense.model.*;
import com.wifisense.repository.AgentKeyRepository;
import com.wifisense.repository.HomeNetworkRepository;
import com.wifisense.repository.WifiScanObservationRepository;
import com.wifisense.repository.WifiScanSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WifiScanServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-26T20:00:00Z");

    private final HomeNetworkRepository networks = mock(HomeNetworkRepository.class);
    private final AgentKeyRepository keys = mock(AgentKeyRepository.class);
    private final WifiScanSessionRepository scans = mock(WifiScanSessionRepository.class);
    private final WifiScanObservationRepository observations = mock(WifiScanObservationRepository.class);
    private final WifiScanQueue queue = mock(WifiScanQueue.class);
    private final AgentProperties agent = new AgentProperties(Duration.ofSeconds(50), Duration.ofDays(30),
            Duration.ofMinutes(15), 20, 48);
    private final WifiScanService service = new WifiScanService(networks, keys, scans, observations, queue,
            WifiScanQueueTest.PROPERTIES, agent, Clock.fixed(NOW, ZoneOffset.UTC));
    private final User ana = new User("ana", "ana@x.io", "x", User.Role.USER);
    private final HomeNetwork casa = new HomeNetwork(ana, "Casa", null, null);
    private final AgentKey key = new AgentKey(casa, "a".repeat(64), "wsa_aaaa");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ana, "id", 3L);
        ReflectionTestUtils.setField(casa, "id", 7L);
        when(networks.findByIdAndOwner_Username(7L, "ana")).thenReturn(Optional.of(casa));
        key.seen(NOW.minusSeconds(120));
        when(keys.findByNetworkId(7L)).thenReturn(Optional.of(key));
        when(queue.enqueue(eq(7L), eq(ScanOrigin.MANUAL), eq(3L), eq(NOW)))
                .thenAnswer(call -> scan(ScanOrigin.MANUAL, NOW, 50L));
    }

    private WifiScanSession scan(ScanOrigin origin, Instant requested, Long id) {
        WifiScanSession scan = new WifiScanSession(casa, origin, null, requested);
        ReflectionTestUtils.setField(scan, "id", id);
        return scan;
    }

    @Test
    void theOwnerAsksForAScanAndItWaitsForTheAgent() {
        WifiScanResponse response = service.request("ana", 7L);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.origin()).isEqualTo("MANUAL");
        verify(queue).enqueue(7L, ScanOrigin.MANUAL, 3L, NOW);
    }

    @Test
    void anotherUserCannotScanOrReadTheNetworkOfAna() {
        when(networks.findByIdAndOwner_Username(7L, "beto")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request("beto", 7L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.nearby("beto", 7L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.history("beto", 7L, 10)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.detail("beto", 7L, 50L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(queue, scans);
    }

    @Test
    void aNetworkWithoutAgentCannotScan() {
        when(keys.findByNetworkId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request("ana", 7L)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Esta red aún no tiene el agente instalado.");
        verify(queue, never()).enqueue(anyLong(), any(), any(), any());
    }

    @Test
    void anAgentThatHasNotReportedLatelyCannotBeAskedToScan() {
        key.seen(NOW.minus(Duration.ofMinutes(16)));

        assertThatThrownBy(() -> service.request("ana", 7L)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no se ha conectado en los últimos 15 minutos");
    }

    @Test
    void theButtonCannotBePressedInALoop() {
        when(scans.findFirstByNetworkIdAndOriginOrderByRequestedAtDesc(7L, ScanOrigin.MANUAL))
                .thenReturn(Optional.of(scan(ScanOrigin.MANUAL, NOW.minusSeconds(10), 49L)));

        assertThatThrownBy(() -> service.request("ana", 7L)).isInstanceOf(ScanLimitException.class)
                .satisfies(e -> assertThat(((ScanLimitException) e).getRetryAfterSeconds()).isEqualTo(21));
    }
}
