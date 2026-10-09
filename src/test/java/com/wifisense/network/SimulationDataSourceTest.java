package com.wifisense.network;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Random;

import static com.wifisense.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;

class SimulationDataSourceTest {

    private static final NetworkTarget TARGET = new NetworkTarget(1, "Test", "A4:2B:B0:10:00:01", "5GHz");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @RepeatedTest(20)
    void producesValidSimulatedReadings() {
        NetworkSnapshot snapshot = new SimulationDataSource(new Random(), 0.5, CLOCK).collect(TARGET);

        assertThat(snapshot.simulated()).isTrue();
        assertThat(snapshot.collectedAt()).isEqualTo(NOW);
        assertThat(snapshot.packetLossPct()).isBetween(0.0, 100.0);
        assertThat(snapshot.signalStrengthDbm()).isBetween(-100, 0);
        assertThat(snapshot.protocolPackets()).containsKeys("HTTPS", "DNS");
        assertThat(snapshot.hasTraffic()).isTrue();
    }

    @Test
    void degradationEpisodesWorsenTheReadings() {
        NetworkSnapshot healthy = new SimulationDataSource(new Random(7), 0.0, CLOCK).collect(TARGET);
        NetworkSnapshot degraded = new SimulationDataSource(new Random(7), 1.0, CLOCK).collect(TARGET);

        assertThat(degraded.latencyMs()).isGreaterThan(healthy.latencyMs() * 2.5);
        assertThat(degraded.packetLossPct()).isGreaterThanOrEqualTo(5.0);
    }
}
