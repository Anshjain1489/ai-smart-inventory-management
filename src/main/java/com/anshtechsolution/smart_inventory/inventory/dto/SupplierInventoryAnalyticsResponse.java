package com.anshtechsolution.smart_inventory.inventory.dto;

import lombok.Builder;

@Builder
public record SupplierInventoryAnalyticsResponse(
        Long supplierId,
        Long totalProducts,
        Long totalStockIn,
        Long totalStockOut,
        Long netStockMovement,
        Long totalInTransactions,
        Long totalOutTransactions
) {
}