package com.anshtechsolution.smart_inventory.analytics.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSummaryResponse {
    private long totalSales;
    private long unitsSold;
    private BigDecimal totalRevenue;
}