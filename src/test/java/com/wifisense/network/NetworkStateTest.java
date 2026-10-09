package com.wifisense.network;

import com.wifisense.model.Alert;
import com.wifisense.model.Network;
import com.wifisense.model.NetworkStatus;
import com.wifisense.network.state.NetworkState;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.wifisense.TestData.NOW;
import static com.wifisense.TestData.network;
import static org.assertj.core.api.Assertions.assertThat;

class NetworkStateTest {

    @Test
    void criticalNetworkRecoversThroughWarning() {
        Network network = network(1, NetworkStatus.CRITICAL);

        assertThat(network.applyDetectedStatus(NetworkStatus.NORMAL)).isTrue();
        assertThat(network.getStatus()).isEqualTo(NetworkStatus.WARNING);

        network.applyDetectedStatus(NetworkStatus.NORMAL);
        assertThat(network.getStatus()).isEqualTo(NetworkStatus.NORMAL);
    }

    @Test
    void normalNetworkCanEscalateDirectlyToCritical() {
        Network network = network(1, NetworkStatus.NORMAL);

        assertThat(network.applyDetectedStatus(NetworkStatus.CRITICAL)).isTrue();
        assertThat(network.getStatus()).isEqualTo(NetworkStatus.CRITICAL);
    }

    @Test
    void sameStatusIsNotAChange() {
        assertThat(network(1, NetworkStatus.WARNING).applyDetectedStatus(NetworkStatus.WARNING)).isFalse();
    }

    @Test
    void worseStatesAreMonitoredMoreOften() {
        Duration normal = NetworkState.of(NetworkStatus.NORMAL).collectionInterval();
        Duration critical = NetworkState.of(NetworkStatus.CRITICAL).collectionInterval();

        assertThat(critical).isLessThan(normal);
    }

    @Test
    void collectionDueDependsOnState() {
        Network critical = network(1, NetworkStatus.CRITICAL);
        Network normal = network(2, NetworkStatus.NORMAL);
        critical.markCollected(NOW.minus(Duration.ofMinutes(2)));
        normal.markCollected(NOW.minus(Duration.ofMinutes(2)));

        assertThat(critical.isCollectionDue(NOW)).isTrue();
        assertThat(normal.isCollectionDue(NOW)).isFalse();
    }

    @Test
    void onlyNormalStateResolvesAlerts() {
        assertThat(NetworkState.of(NetworkStatus.NORMAL).resolvesOpenAlerts()).isTrue();
        assertThat(NetworkState.of(NetworkStatus.CRITICAL).alertSeverityOnEnter()).isEqualTo(Alert.Severity.CRITICAL);
    }
}
