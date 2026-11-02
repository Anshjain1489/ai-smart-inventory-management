package com.anshtechsolution.smart_inventory.inventory.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record StockMovementResponse(
        Long id,
        Long productId,
        String productName,
        String movementType,
        Integer quantity,
        Integer currentStock,
        String reference,
        String notes,
        LocalDateTime createdAt
) {
}