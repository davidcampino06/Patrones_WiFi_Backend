package com.wifisense.event;

import com.wifisense.model.Alert;
import com.wifisense.model.AnalysisResult;
import com.wifisense.model.Network;
import com.wifisense.network.state.NetworkState;
import com.wifisense.repository.AlertRepository;
import com.wifisense.repository.AnalysisResultRepository;
import com.wifisense.repository.NetworkRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/** Turns status transitions into alerts; the new state decides severity and whether old alerts close. */
@Component
public class AlertObserver implements NetworkEventListener {

    private final AlertRepository alerts;
    private final NetworkRepository networks;
    private final AnalysisResultRepository analysisResults;
    private final Clock clock;

    public AlertObserver(AlertRepository alerts, NetworkRepository networks,
                         AnalysisResultRepository analysisResults, Clock clock) {
        this.alerts = alerts;
        this.networks = networks;
        this.analysisResults = analysisResults;
        this.clock = clock;
    }

    @Override
    public void onEvent(NetworkEvent event) {
        if (event instanceof NetworkStatusChangedEvent changed) {
            raiseAlert(changed);
        }
    }

    private void raiseAlert(NetworkStatusChangedEvent event) {
        NetworkState state = NetworkState.of(event.current());
        Network network = networks.getReferenceById(event.networkId());
        AnalysisResult result = event.analysisResultId() == null
                ? null : analysisResults.getReferenceById(event.analysisResultId());

        if (state.resolvesOpenAlerts()) {
            Instant now = Instant.now(clock);
            alerts.findByNetworkIdAndStatusNot(event.networkId(), Alert.Status.RESOLVED)
                    .forEach(alert -> alert.resolve(now));
        }
        String message = "%s pasó de %s a %s. %s".formatted(event.ssid(), event.previous().label(), event.current().label(),
                event.reason());
        alerts.save(new Alert(network, result, state.alertSeverityOnEnter(), truncate(message)));
    }

    private static String truncate(String text) {
        return text.length() <= 300 ? text : text.substring(0, 297) + "...";
    }
}
