package com.wifisense.service;

import java.util.List;

public class InvalidAccountException extends RuntimeException {

    private final List<String> errors;

    public InvalidAccountException(List<String> errors) {
        super("Revisa los datos de la cuenta.");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
