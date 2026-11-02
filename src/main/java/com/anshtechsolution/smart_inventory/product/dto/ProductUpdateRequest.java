package com.anshtechsolution.smart_inventory.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {

    @NotBlank
    @Size(max = 60)
    private String sku;

    @NotBlank
    @Size(max = 200)
    private String name;

    private String description;

    @Size(max = 100)
    private String category;

    @Positive
    private Long supplierId;

    @NotNull
    @PositiveOrZero
    private Integer quantity;

    @NotNull
    @PositiveOrZero
    private Integer reorderLevel;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal unitPrice;
}
