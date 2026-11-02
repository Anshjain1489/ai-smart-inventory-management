package com.anshtechsolution.smart_inventory.purchaseorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PurchaseOrderItemRequestDTO(

        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        Long productId,

        @NotNull(message = "Ordered quantity is required")
        @Positive(message = "Ordered quantity must be greater than zero")
        Integer orderedQuantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00",
                message = "Unit price cannot be negative")
        BigDecimal unitPrice

) {
}