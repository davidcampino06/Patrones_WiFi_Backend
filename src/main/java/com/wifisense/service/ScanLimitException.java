package com.wifisense.service;

/**
 * A scan was asked for too soon (by the user, or the agent asked for pending scans too often).
 * {@code retryAfterSeconds} goes to the Retry-After header.
 */
public class ScanLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public ScanLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
