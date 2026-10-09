package com.wifisense.service;

import com.wifisense.dto.AuthResponse;
import com.wifisense.dto.LoginRequest;
import com.wifisense.security.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;

    public AuthService(AuthenticationManager authenticationManager, TokenService tokenService,
                       UserService userService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userService = userService;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        TokenService.IssuedToken token = tokenService.issue(authentication);
        return new AuthResponse(token.value(), token.expiresAt(), userService.findByUsername(authentication.getName()));
    }
}
