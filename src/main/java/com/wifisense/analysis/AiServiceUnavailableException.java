package com.wifisense.analysis;

public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    /** The service was not called at all (the circuit breaker knows it is down). */
    public AiServiceUnavailableException(String message) {
        super(message);
    }
}
