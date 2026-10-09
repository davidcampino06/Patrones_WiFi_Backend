package com.wifisense.network.state;

import com.wifisense.model.Alert;
import com.wifisense.model.NetworkStatus;

import java.time.Duration;

/** State pattern: behaviour that changes with the network's health. */
public interface NetworkState {

    NetworkStatus status();

    /** Next status given what the latest analysis detected (states may smooth transitions). */
    NetworkStatus next(NetworkStatus detected);

    /** How often the monitoring job collects data while in this state. */
    Duration collectionInterval();

    /** Severity of the alert raised when a network enters this state. */
    Alert.Severity alertSeverityOnEnter();

    /** Whether entering this state closes the network's open alerts. */
    boolean resolvesOpenAlerts();

    static NetworkState of(NetworkStatus status) {
        return switch (status) {
            case NORMAL -> NormalState.INSTANCE;
            case WARNING -> WarningState.INSTANCE;
            case CRITICAL -> CriticalState.INSTANCE;
        };
    }
}
