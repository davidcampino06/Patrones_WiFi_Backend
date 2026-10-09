package com.wifisense.model;

public enum NetworkStatus {
    NORMAL, WARNING, CRITICAL;

    /** Spanish label for messages shown to users. */
    public String label() {
        return switch (this) {
            case NORMAL -> "normal";
            case WARNING -> "advertencia";
            case CRITICAL -> "crítico";
        };
    }

    public boolean isWorseThan(NetworkStatus other) {
        return ordinal() > other.ordinal();
    }

    public static NetworkStatus worst(NetworkStatus a, NetworkStatus b) {
        return a.isWorseThan(b) ? a : b;
    }
}
