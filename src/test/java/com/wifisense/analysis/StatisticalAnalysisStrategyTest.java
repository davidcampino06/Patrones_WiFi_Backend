package com.wifisense.analysis;

import com.wifisense.model.Measurement;
import com.wifisense.model.Network;
import com.wifisense.model.NetworkStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.wifisense.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatisticalAnalysisStrategyTest {

    private final StatisticalAnalysisStrategy strategy = new StatisticalAnalysisStrategy();
    private final Network network = network(1, NetworkStatus.NORMAL);

    private List<Measurement> stableHistory(int size) {
        List<Measurement> history = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            history.add(measurement(network, 20 + (i % 3), 3 + (i % 2), 0.2 + (i % 2) * 0.1, -55 - (i % 3),
                    NOW.minus(Duration.ofMinutes(size - i))));
        }
        return history;
    }

    @Test
    void consistentMeasurementIsNormal() {
        List<Measurement> history = stableHistory(20);
        history.add(measurement(network, 21, 3, 0.2, -56, NOW));

        AnalysisOutcome outcome = strategy.analyze(new AnalysisContext(1, "Test", history, List.of()));

        assertThat(outcome.status()).isEqualTo(NetworkStatus.NORMAL);
    }

    @Test
    void strongDeviationIsCritical() {
        List<Measurement> history = stableHistory(20);
        history.add(measurement(network, 90, 3, 0.2, -56, NOW));

        AnalysisOutcome outcome = strategy.analyze(new AnalysisContext(1, "Test", history, List.of()));

        assertThat(outcome.status()).isEqualTo(NetworkStatus.CRITICAL);
        assertThat(outcome.summary()).contains("latency");
    }

    @Test
    void improvementIsNotHarmful() {
        assertThat(StatisticalAnalysisStrategy.harmfulZScore(1, List.of(20.0, 22.0, 18.0), 1)).isZero();
    }

    @Test
    void requiresEnoughHistory() {
        AnalysisContext context = new AnalysisContext(1, "Test", stableHistory(5), List.of());

        assertThatThrownBy(() -> strategy.analyze(context)).isInstanceOf(InsufficientDataException.class);
    }
}
