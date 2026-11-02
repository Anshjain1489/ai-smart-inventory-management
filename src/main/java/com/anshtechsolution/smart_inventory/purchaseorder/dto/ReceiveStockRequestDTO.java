package com.anshtechsolution.smart_inventory.purchaseorder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReceiveStockRequestDTO(

        @NotNull(message = "Received quantity is required")
        @Positive(message = "Received quantity must be greater than zero")
        Integer receivedQuantity

) {
}