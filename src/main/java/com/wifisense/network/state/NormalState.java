package com.wifisense.network.state;

import com.wifisense.model.Alert;
import com.wifisense.model.NetworkStatus;

import java.time.Duration;

public final class NormalState implements NetworkState {

    static final NormalState INSTANCE = new NormalState();

    private NormalState() {
    }

    @Override
    public NetworkStatus status() {
        return NetworkStatus.NORMAL;
    }

    @Override
    public NetworkStatus next(NetworkStatus detected) {
        return detected;
    }

    @Override
    public Duration collectionInterval() {
        return Duration.ofMinutes(5);
    }

    @Override
    public Alert.Severity alertSeverityOnEnter() {
        return Alert.Severity.INFO;
    }

    @Override
    public boolean resolvesOpenAlerts() {
        return true;
    }
}
