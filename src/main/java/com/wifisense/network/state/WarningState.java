package com.wifisense.network.state;

import com.wifisense.model.Alert;
import com.wifisense.model.NetworkStatus;

import java.time.Duration;

public final class WarningState implements NetworkState {

    static final WarningState INSTANCE = new WarningState();

    private WarningState() {
    }

    @Override
    public NetworkStatus status() {
        return NetworkStatus.WARNING;
    }

    @Override
    public NetworkStatus next(NetworkStatus detected) {
        return detected;
    }

    @Override
    public Duration collectionInterval() {
        return Duration.ofMinutes(2);
    }

    @Override
    public Alert.Severity alertSeverityOnEnter() {
        return Alert.Severity.WARNING;
    }

    @Override
    public boolean resolvesOpenAlerts() {
        return false;
    }
}
