package com.wifisense.event;

import com.wifisense.analysis.AnalysisType;
import com.wifisense.model.Alert;
import com.wifisense.model.Network;
import com.wifisense.model.NetworkStatus;
import com.wifisense.repository.AlertRepository;
import com.wifisense.repository.AnalysisResultRepository;
import com.wifisense.repository.NetworkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.wifisense.TestData.NOW;
import static com.wifisense.TestData.network;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ObserverTest {

    private final AlertRepository alerts = mock(AlertRepository.class);
    private final NetworkRepository networks = mock(NetworkRepository.class);
    private final AlertObserver alertObserver = new AlertObserver(alerts, networks,
            mock(AnalysisResultRepository.class), Clock.fixed(NOW, ZoneOffset.UTC));

    private static NetworkStatusChangedEvent change(NetworkStatus from, NetworkStatus to) {
        return new NetworkStatusChangedEvent(1, "Test", from, to, null, "reason", NOW);
    }

    @Test
    void publisherNotifiesEveryListener() {
        NetworkEventListener first = mock(NetworkEventListener.class);
        NetworkEventListener second = mock(NetworkEventListener.class);
        NetworkEvent event = change(NetworkStatus.NORMAL, NetworkStatus.WARNING);

        new NetworkEventPublisher(List.of(first, second)).publish(event);

        verify(first).onEvent(event);
        verify(second).onEvent(event);
    }

    @Test
    void escalationCreatesAlertWithSeverityOfNewState() {
        when(networks.getReferenceById(1L)).thenReturn(network(1, NetworkStatus.CRITICAL));

        alertObserver.onEvent(change(NetworkStatus.NORMAL, NetworkStatus.CRITICAL));

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alerts).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(Alert.Severity.CRITICAL);
        verify(alerts, never()).findByNetworkIdAndStatusNot(anyLong(), any());
    }

    @Test
    void recoveryResolvesOpenAlerts() {
        Network network = network(1, NetworkStatus.NORMAL);
        Alert open = new Alert(network, null, Alert.Severity.WARNING, "old");
        when(networks.getReferenceById(1L)).thenReturn(network);
        when(alerts.findByNetworkIdAndStatusNot(1L, Alert.Status.RESOLVED)).thenReturn(List.of(open));

        alertObserver.onEvent(change(NetworkStatus.WARNING, NetworkStatus.NORMAL));

        assertThat(open.getStatus()).isEqualTo(Alert.Status.RESOLVED);
    }

    @Test
    void otherEventsDoNotCreateAlerts() {
        alertObserver.onEvent(new AnalysisCompletedEvent(1, "Test", 5, AnalysisType.THRESHOLD,
                NetworkStatus.NORMAL, 0.1, NOW));

        verifyNoInteractions(alerts);
    }

    @Test
    void activityLogKeepsOnlyTheMostRecentEvents() {
        ActivityLogObserver log = new ActivityLogObserver();
        for (int i = 0; i < ActivityLogObserver.CAPACITY + 10; i++) {
            log.onEvent(new NetworkStatusChangedEvent(i, "Net" + i, NetworkStatus.NORMAL, NetworkStatus.WARNING,
                    null, "r", NOW));
        }

        List<ActivityLogObserver.ActivityEntry> recent = log.recentActivity();
        assertThat(recent).hasSize(ActivityLogObserver.CAPACITY);
        assertThat(recent.getFirst().networkId()).isEqualTo(ActivityLogObserver.CAPACITY + 9);
    }
}
