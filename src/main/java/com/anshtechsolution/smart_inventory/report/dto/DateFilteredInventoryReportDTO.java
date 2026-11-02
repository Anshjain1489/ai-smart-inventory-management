package com.anshtechsolution.smart_inventory.report.dto;

import java.math.BigDecimal;

public record DateFilteredInventoryReportDTO(
        Long totalProducts,
        Long totalUnits,
        BigDecimal totalInventoryValue
) {
}