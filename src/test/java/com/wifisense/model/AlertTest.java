package com.wifisense.model;

import org.junit.jupiter.api.Test;

import static com.wifisense.TestData.NOW;
import static com.wifisense.TestData.network;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertTest {

    private final Alert alert = new Alert(network(1, NetworkStatus.WARNING), null, Alert.Severity.WARNING, "x");

    @Test
    void followsOpenAcknowledgedResolvedLifecycle() {
        alert.acknowledge();
        alert.resolve(NOW);

        assertThat(alert.getStatus()).isEqualTo(Alert.Status.RESOLVED);
        assertThat(alert.getResolvedAt()).isEqualTo(NOW);
    }

    @Test
    void cannotAcknowledgeTwiceOrResolveTwice() {
        alert.acknowledge();
        assertThatThrownBy(alert::acknowledge).isInstanceOf(IllegalStateException.class);

        alert.resolve(NOW);
        assertThatThrownBy(() -> alert.resolve(NOW)).isInstanceOf(IllegalStateException.class);
    }
}
