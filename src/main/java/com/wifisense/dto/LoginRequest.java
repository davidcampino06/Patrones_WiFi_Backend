package com.wifisense.dto;

/** No bean validation here: any malformed login is answered with the same generic error. */
public record LoginRequest(String username, String password) {
}
