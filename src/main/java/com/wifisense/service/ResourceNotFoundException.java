package com.wifisense.service;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("No se encontró " + resource + " con id " + id);
    }

    /** A sentence of its own, when there is no id to name (for example, a feature that does not exist for a key). */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
