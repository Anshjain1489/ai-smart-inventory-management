package com.anshtechsolution.smart_inventory.inventory.dto;

import lombok.Builder;

@Builder
public record InventoryDashboardResponse(
        Long totalProducts,
        Long totalInventoryQuantity,
        Long lowStockProducts,
        Long totalStockIn,
        Long totalStockOut,
        Long netStockMovement,
        Long totalInTransactions,
        Long totalOutTransactions
) {
}