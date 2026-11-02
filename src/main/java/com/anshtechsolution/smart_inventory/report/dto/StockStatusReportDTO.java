package com.anshtechsolution.smart_inventory.report.dto;

public record StockStatusReportDTO(
        Long inStockProducts,
        Long lowStockProducts,
        Long outOfStockProducts,
        Long totalProducts
) {
}