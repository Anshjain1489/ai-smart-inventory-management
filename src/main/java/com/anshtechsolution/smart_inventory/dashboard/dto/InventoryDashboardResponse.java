package com.anshtechsolution.smart_inventory.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDashboardResponse {

    private Long totalProducts;

    private Long totalUnitsInStock;

    private Long lowStockProducts;

    private Long outOfStockProducts;

    private BigDecimal totalStockValueAtUnitPrice;

    private Long overstockAlerts;

    private Long productsWithNoSalesData;

    private LocalDateTime generatedAt;
}