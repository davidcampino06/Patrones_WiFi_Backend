package com.wifisense.model;

public enum NetworkStatus {
    NORMAL, WARNING, CRITICAL;

    public boolean isWorseThan(NetworkStatus other) {
        return ordinal() > other.ordinal();
    }

    public static NetworkStatus worst(NetworkStatus a, NetworkStatus b) {
        return a.isWorseThan(b) ? a : b;
    }
}
