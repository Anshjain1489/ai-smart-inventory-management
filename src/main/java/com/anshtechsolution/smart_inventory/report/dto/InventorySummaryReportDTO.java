package com.anshtechsolution.smart_inventory.report.dto;

import java.math.BigDecimal;

public record InventorySummaryReportDTO(
        Long totalProducts,
        Long totalUnits,
        BigDecimal totalInventoryValue,
        Long totalCategories
) {
}