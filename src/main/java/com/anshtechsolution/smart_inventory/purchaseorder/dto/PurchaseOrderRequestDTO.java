package com.anshtechsolution.smart_inventory.purchaseorder.dto;
import com.anshtechsolution.smart_inventory.purchaseorder.dto.PurchaseOrderItemRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderRequestDTO(

        @NotNull(message = "Supplier ID is required")
        @Positive(message = "Supplier ID must be positive")
        Long supplierId,

        LocalDateTime expectedDeliveryDate,

        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String notes,

        @NotEmpty(message = "At least one purchase order item is required")
        List<@Valid PurchaseOrderItemRequestDTO> items

) {
}