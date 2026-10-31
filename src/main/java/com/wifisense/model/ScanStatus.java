package com.wifisense.model;

/**
 * Life of a scan of the nearby Wi-Fi networks: PENDING (asked, waiting for the agent), RUNNING (the agent picked it
 * up), then COMPLETED or FAILED. A finished scan never changes again.
 */
public enum ScanStatus {
    PENDING, RUNNING, COMPLETED, FAILED;

    /** Waiting or running: a network has at most one such scan. */
    public boolean active() {
        return this == PENDING || this == RUNNING;
    }

    /** The only moves a scan can make: forward, and never out of a finished state. */
    public boolean canMoveTo(ScanStatus next) {
        return switch (this) {
            case PENDING -> next == RUNNING || next == COMPLETED || next == FAILED;
            case RUNNING -> next == COMPLETED || next == FAILED;
            case COMPLETED, FAILED -> false;
        };
    }
}
