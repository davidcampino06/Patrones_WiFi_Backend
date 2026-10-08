package com.wifisense.dto;

import com.wifisense.model.User;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull User.Role role) {
}
