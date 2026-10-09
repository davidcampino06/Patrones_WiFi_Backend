package com.wifisense.security;

public class LoginBlockedException extends RuntimeException {

    public LoginBlockedException() {
        super("Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo.");
    }
}
