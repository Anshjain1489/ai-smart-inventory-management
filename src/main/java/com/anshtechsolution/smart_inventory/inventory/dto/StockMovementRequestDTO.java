package com.anshtechsolution.smart_inventory.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockMovementRequestDTO(

        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        Long productId,

        @NotBlank(message = "Movement type is required")
        String movementType,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity,

        @Size(max = 100, message = "Reference cannot exceed 100 characters")
        String reference,

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes,

        @Size(max = 100, message = "Idempotency key cannot exceed 100 characters")
        String idempotencyKey

) {
}