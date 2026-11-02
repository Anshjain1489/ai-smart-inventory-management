package com.anshtechsolution.smart_inventory.report.dto;

import java.math.BigDecimal;

public record InventoryValuationItemDTO(
        Long productId,
        String sku,
        String name,
        String category,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal inventoryValue
) {
}