package com.wifisense.service;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("No se encontró " + resource + " con id " + id);
    }
}
