package com.anshtechsolution.smart_inventory.inventory.dto;

import com.anshtechsolution.smart_inventory.inventory.InventoryAlertType;
import com.anshtechsolution.smart_inventory.inventory.InventoryRiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertResponse {

    private Long productId;
    private String productName;
    private String sku;

    private InventoryAlertType alertType;
    private InventoryRiskLevel riskLevel;

    private Integer currentStock;
    private Integer reorderLevel;

    private String message;
    private LocalDateTime createdAt;
}