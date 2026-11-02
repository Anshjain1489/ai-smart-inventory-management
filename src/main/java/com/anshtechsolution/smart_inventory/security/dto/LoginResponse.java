package com.anshtechsolution.smart_inventory.security.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}