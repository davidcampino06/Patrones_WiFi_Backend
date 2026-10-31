package com.wifisense.service;

import com.wifisense.dto.AgentScanResult;
import com.wifisense.model.*;
import com.wifisense.repository.WifiAccessPointRepository;
import com.wifisense.repository.WifiScanObservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WifiScanRecorderTest {

    private static final Instant NOW = Instant.parse("2026-10-26T20:00:00Z");

    private final WifiAccessPointRepository accessPoints = mock(WifiAccessPointRepository.class);
    private final WifiScanObservationRepository observations = mock(WifiScanObservationRepository.class);
    private final WifiScanRecorder recorder = new WifiScanRecorder(accessPoints, observations,
            WifiScanQueueTest.PROPERTIES);
    private final HomeNetwork casa = mock(HomeNetwork.class);
    private final WifiScanSession scan = new WifiScanSession(casa, ScanOrigin.MANUAL, null, NOW.minusSeconds(20));

    static AgentScanResult.AccessPoint ap(String fingerprint, String ssid, Integer signal) {
        return new AgentScanResult.AccessPoint(fingerprint, "F1", "TP-Link", ssid, signal, null, 6, "2.4", "802.11n",
                "WPA2", "WPA2-Personal", "CCMP", false);
    }

    @BeforeEach
    void setUp() {
        when(casa.getId()).thenReturn(7L);
        when(accessPoints.save(any(WifiAccessPoint.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void newAccessPointsAreStoredWithOneObservationEach() {
        when(accessPoints.findByNetworkIdAndBssidFingerprintIn(eq(7L), anyCollection())).thenReturn(List.of());

        WifiScanRecorder.Stored stored = recorder.record(scan,
                List.of(ap("a".repeat(24), "Casa", 80), ap("b".repeat(24), "Casa", 60), ap("c".repeat(24), "Café", 40)),
                NOW);

        assertThat(stored.accessPoints()).isEqualTo(3);
        assertThat(stored.networks()).isEqualTo(2);  // "Casa" has two access points
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WifiScanObservation>> rows = ArgumentCaptor.forClass(List.class);
        verify(observations).saveAll(rows.capture());
        assertThat(rows.getValue()).hasSize(3);
        assertThat(rows.getValue().get(0).getSignalPercent()).isEqualTo(80);
        assertThat(rows.getValue().get(0).getSignalDbm()).isNull();  // never calculated from the percentage
        assertThat(rows.getValue().get(0).getAccessPoint().getTimesSeen()).isEqualTo(1);
    }

    @Test
    void anAccessPointSeenBeforeIsUpdatedNotCopied() {
        WifiAccessPoint known = new WifiAccessPoint(casa, "a".repeat(24), NOW.minusSeconds(7200));
        known.seen("Casa", "TP-Link", "F1", NOW.minusSeconds(7200));
        ReflectionTestUtils.setField(known, "id", 11L);
        when(accessPoints.findByNetworkIdAndBssidFingerprintIn(eq(7L), anyCollection())).thenReturn(List.of(known));

        recorder.record(scan, List.of(ap("a".repeat(24), "Casa", 75)), NOW);

        verify(accessPoints).save(known);
        assertThat(known.getTimesSeen()).isEqualTo(2);
        assertThat(known.getLastSeenAt()).isEqualTo(NOW);
        assertThat(known.getFirstSeenAt()).isEqualTo(NOW.minusSeconds(7200));
    }

    @Test
    void aRepeatedBssidKeepsItsStrongestReading() {
        List<AgentScanResult.AccessPoint> kept = WifiScanRecorder.keep(
                List.of(ap("a".repeat(24), "Casa", 30), ap("a".repeat(24), "Casa", 70), ap("b".repeat(24), "X", 50)),
                200);

        assertThat(kept).hasSize(2);
        assertThat(kept.get(0).signalPercent()).isEqualTo(70);
    }

    @Test
    void onlyTheStrongestAccessPointsAreKeptAndUnknownSignalGoesLast() {
        List<AgentScanResult.AccessPoint> seen = new ArrayList<>();
        seen.add(ap("0".repeat(24), "Sin señal", null));
        for (int i = 1; i <= 5; i++) {
            seen.add(ap(String.valueOf(i).repeat(24), "Red " + i, i * 10));
        }

        List<AgentScanResult.AccessPoint> kept = WifiScanRecorder.keep(seen, 3);

        assertThat(kept).extracting(AgentScanResult.AccessPoint::signalPercent).containsExactly(50, 40, 30);
    }

    @Test
    void eachHiddenAccessPointCountsAsOneNetwork() {
        assertThat(WifiScanRecorder.distinctNetworks(List.of(ap("a".repeat(24), null, 50),
                ap("b".repeat(24), " ", 40), ap("c".repeat(24), "Casa", 30), ap("d".repeat(24), "Casa", 20))))
                .isEqualTo(3);
    }

    @Test
    void aScanThatSawNothingStoresNothing() {
        WifiScanRecorder.Stored stored = recorder.record(scan, List.of(), NOW);

        assertThat(stored).isEqualTo(new WifiScanRecorder.Stored(0, 0));
        verify(accessPoints, never()).save(any());
    }

    @Test
    void controlCharactersInTheNameAreNotStored() {
        when(accessPoints.findByNetworkIdAndBssidFingerprintIn(eq(7L), anyCollection())).thenReturn(List.of());

        recorder.record(scan, List.of(ap("a".repeat(24), "Red\u0000\u001b[31m roja", 50)), NOW);

        ArgumentCaptor<WifiAccessPoint> stored = ArgumentCaptor.forClass(WifiAccessPoint.class);
        verify(accessPoints).save(stored.capture());
        assertThat(stored.getValue().getSsid()).isEqualTo("Red[31m roja");
    }
}
