package com.wifisense.controller;

import com.wifisense.dto.RoleUpdateRequest;
import com.wifisense.dto.UserResponse;
import com.wifisense.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return userService.changeRole(id, request.role());
    }
}
