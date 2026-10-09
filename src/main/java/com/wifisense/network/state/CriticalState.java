package com.wifisense.network.state;

import com.wifisense.model.Alert;
import com.wifisense.model.NetworkStatus;

import java.time.Duration;

public final class CriticalState implements NetworkState {

    static final CriticalState INSTANCE = new CriticalState();

    private CriticalState() {
    }

    @Override
    public NetworkStatus status() {
        return NetworkStatus.CRITICAL;
    }

    /** A critical network must recover through WARNING; one good reading is not enough to call it normal. */
    @Override
    public NetworkStatus next(NetworkStatus detected) {
        return detected == NetworkStatus.NORMAL ? NetworkStatus.WARNING : detected;
    }

    @Override
    public Duration collectionInterval() {
        return Duration.ofMinutes(1);
    }

    @Override
    public Alert.Severity alertSeverityOnEnter() {
        return Alert.Severity.CRITICAL;
    }

    @Override
    public boolean resolvesOpenAlerts() {
        return false;
    }
}
