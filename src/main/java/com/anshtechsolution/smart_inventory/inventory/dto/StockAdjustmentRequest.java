package com.anshtechsolution.smart_inventory.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentRequest {

    @NotNull
    @Positive
    private Integer quantity;

    @Size(max = 100)
    private String reference;

    @Size(max = 500)
    private String notes;
}