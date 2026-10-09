package com.wifisense.dto;

import com.wifisense.model.User;

public record UserResponse(Long id, String username, User.Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
