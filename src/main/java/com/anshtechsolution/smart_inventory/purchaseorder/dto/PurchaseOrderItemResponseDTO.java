package com.anshtechsolution.smart_inventory.purchaseorder.dto;

import java.math.BigDecimal;

public record PurchaseOrderItemResponseDTO(
        Long id,
        Long productId,
        String productName,
        Integer orderedQuantity,
        Integer receivedQuantity,
        Integer pendingQuantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}