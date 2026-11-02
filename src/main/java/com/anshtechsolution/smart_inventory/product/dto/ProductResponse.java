package com.anshtechsolution.smart_inventory.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private Long supplierId;
    private Integer quantity;
    private Integer reorderLevel;
    private BigDecimal unitPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}