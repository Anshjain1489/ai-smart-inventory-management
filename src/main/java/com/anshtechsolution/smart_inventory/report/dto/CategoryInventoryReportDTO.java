package com.anshtechsolution.smart_inventory.report.dto;

import java.math.BigDecimal;

public record CategoryInventoryReportDTO(
        String category,
        Long totalProducts,
        Long totalUnits,
        BigDecimal totalInventoryValue
) {
}