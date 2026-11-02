package com.anshtechsolution.smart_inventory.analytics.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductResponse {
    private Long productId;
    private String productName;
    private Long unitsSold;
    private BigDecimal revenue;
}