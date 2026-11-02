package com.anshtechsolution.smart_inventory.purchaseorder.dto;

import com.anshtechsolution.smart_inventory.purchaseorder.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponseDTO(
        Long id,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        LocalDateTime orderDate,
        LocalDateTime expectedDeliveryDate,
        BigDecimal totalAmount,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PurchaseOrderItemResponseDTO> items
) {
}