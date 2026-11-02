package com.anshtechsolution.smart_inventory.analytics.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySalesResponse {
    private LocalDate date;
    private Long totalSales;
    private BigDecimal revenue;
}