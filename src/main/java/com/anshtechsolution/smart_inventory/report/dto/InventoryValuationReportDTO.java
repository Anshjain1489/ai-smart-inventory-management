package com.anshtechsolution.smart_inventory.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record InventoryValuationReportDTO(
        Long totalProducts,
        Long totalUnits,
        BigDecimal totalInventoryValue,
        List<InventoryValuationItemDTO> products
) {
}