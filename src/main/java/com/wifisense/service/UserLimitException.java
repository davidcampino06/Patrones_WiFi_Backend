package com.wifisense.service;

public class UserLimitException extends RuntimeException {

    public UserLimitException(int maxUsers) {
        super("Se alcanzó el máximo de " + maxUsers + " usuarios. Elimina uno antes de crear otro.");
    }
}
