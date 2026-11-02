package com.anshtechsolution.smart_inventory.inventory.dto;

import lombok.Builder;

@Builder
public record InventoryAnalyticsResponse(
        Long totalStockIn,
        Long totalStockOut,
        Long netStockMovement,
        Long totalInTransactions,
        Long totalOutTransactions
) {
}