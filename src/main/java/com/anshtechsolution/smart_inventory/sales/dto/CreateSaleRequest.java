package com.anshtechsolution.smart_inventory.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSaleRequest {

    @Size(max = 150)
    private String customerName;

    @NotEmpty
    @Valid
    private List<SaleItemRequest> items;
}