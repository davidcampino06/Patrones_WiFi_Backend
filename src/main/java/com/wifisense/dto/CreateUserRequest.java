package com.wifisense.dto;

import com.wifisense.model.User;
import jakarta.validation.constraints.NotNull;

/** Username and password rules are checked by PasswordPolicy so every message is consistent and in Spanish. */
public record CreateUserRequest(String username, String password, @NotNull User.Role role) {
}
