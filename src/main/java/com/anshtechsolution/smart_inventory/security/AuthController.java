package com.anshtechsolution.smart_inventory.security;

import com.anshtechsolution.smart_inventory.security.dto.LoginRequest;
import com.anshtechsolution.smart_inventory.security.dto.LoginResponse;
import com.anshtechsolution.smart_inventory.security.dto.SignupRequest;
import com.anshtechsolution.smart_inventory.security.dto.SignupResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthService authService;

    // Login existing user
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        String token = jwtService.generateToken(authentication);

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds()
        );

        return ResponseEntity.ok(response);
    }

    // Register a new user
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request) {

        SignupResponse response = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}