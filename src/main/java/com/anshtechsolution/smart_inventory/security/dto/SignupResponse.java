package com.anshtechsolution.smart_inventory.security.dto;

public record SignupResponse(
        Long id,
        String username,
        String role,
        String message
) {
}