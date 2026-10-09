package com.wifisense.analysis;

import com.wifisense.model.Measurement;
import com.wifisense.model.Network;
import com.wifisense.model.NetworkStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.wifisense.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

class ThresholdAnalysisStrategyTest {

    private final ThresholdAnalysisStrategy strategy = new ThresholdAnalysisStrategy(
            new AnalysisProperties(50, new AnalysisProperties.Thresholds(60, 150, 15, 40, 2, 8, -70, -82)));
    private final Network network = network(1, NetworkStatus.NORMAL);

    private AnalysisOutcome analyze(Measurement latest) {
        return strategy.analyze(new AnalysisContext(1, "Test", List.of(latest), List.of()));
    }

    @Test
    void healthyMeasurementIsNormal() {
        AnalysisOutcome outcome = analyze(measurement(network, 20, 3, 0.2, -55, NOW));

        assertThat(outcome.status()).isEqualTo(NetworkStatus.NORMAL);
        assertThat(outcome.score()).isLessThan(0.5);
    }

    @Test
    void worstMetricDecidesTheStatus() {
        AnalysisOutcome outcome = analyze(measurement(network, 80, 3, 9, -55, NOW));

        assertThat(outcome.status()).isEqualTo(NetworkStatus.CRITICAL);
        assertThat(outcome.summary()).contains("latencia", "pérdida de paquetes", "crítico");
        assertThat(outcome.score()).isEqualTo(1.0);
    }

    @Test
    void weakSignalIsDetectedEvenThoughLowerIsWorse() {
        AnalysisOutcome outcome = analyze(measurement(network, 20, 3, 0.2, -75, NOW));

        assertThat(outcome.status()).isEqualTo(NetworkStatus.WARNING);
        assertThat(outcome.summary()).contains("señal");
    }

    @Test
    void missingSignalIsIgnored() {
        assertThat(analyze(measurement(network, 20, 3, 0.2, null, NOW)).status()).isEqualTo(NetworkStatus.NORMAL);
    }
}
