package com.anshtechsolution.smart_inventory.inventory.dto;

import lombok.Builder;

@Builder
public record ProductInventoryAnalyticsResponse(
        Long productId,
        String productName,
        Long totalStockIn,
        Long totalStockOut,
        Long netStockMovement,
        Long totalInTransactions,
        Long totalOutTransactions
) {
}