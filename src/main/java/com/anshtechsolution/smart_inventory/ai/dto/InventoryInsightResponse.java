package com.anshtechsolution.smart_inventory.ai.dto;

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
public class InventoryInsightResponse {

    // Inventory summary metrics calculated by Java
    private Long totalProducts;
    private Long totalUnitsInStock;
    private Long lowStockProducts;
    private BigDecimal totalStockValueAtUnitPrice;

    // AI-generated inventory analysis
    private String analysis;

    // Response generation timestamp
    private LocalDateTime generatedAt;

}