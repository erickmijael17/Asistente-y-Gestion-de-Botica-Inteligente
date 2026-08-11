package com.botica.inteligente.security.auth.controller;

import com.botica.inteligente.security.auth.AuthLoginRequest;
import com.botica.inteligente.security.auth.AuthRegisterRequest;
import com.botica.inteligente.security.auth.AuthResponse;
import com.botica.inteligente.security.auth.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody AuthRegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthLoginRequest request
    ) {
        return ResponseEntity.ok(service.login(request));
    }
}
