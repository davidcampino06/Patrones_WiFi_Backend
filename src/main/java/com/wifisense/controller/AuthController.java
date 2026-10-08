package com.wifisense.controller;

import com.wifisense.dto.AuthResponse;
import com.wifisense.dto.LoginRequest;
import com.wifisense.dto.RegisterRequest;
import com.wifisense.dto.UserResponse;
import com.wifisense.service.AuthService;
import com.wifisense.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }
}
