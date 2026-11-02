package com.anshtechsolution.smart_inventory.sales;

import com.anshtechsolution.smart_inventory.sales.dto.CreateSaleRequest;
import com.anshtechsolution.smart_inventory.sales.dto.SaleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse createSale(
            @Valid @RequestBody CreateSaleRequest request) {
        return salesService.createSale(request);
    }

    @GetMapping("/{id}")
    public SaleResponse getSale(@PathVariable Long id) {
        return salesService.getSale(id);
    }
}