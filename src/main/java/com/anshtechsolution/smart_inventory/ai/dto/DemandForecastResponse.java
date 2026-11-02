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
public class DemandForecastResponse {

    // Product identification
    private Long productId;
    private String productName;

    // Historical analysis period
    private Integer historicalDays;
    private Integer forecastDays;

    // Backend-calculated metrics
    private Integer currentStock;
    private Long unitsSold;
    private BigDecimal averageDailyDemand;
    private Integer estimatedDemand;
    private Integer estimatedStockGap;

    // AI-generated explanation
    private String analysis;

    // Response timestamp
    private LocalDateTime generatedAt;

}