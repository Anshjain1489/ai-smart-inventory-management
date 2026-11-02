package com.anshtechsolution.smart_inventory.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Component
public class ProductSortValidator {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "sku",
            "quantity",
            "unitPrice",
            "createdAt",
            "updatedAt",
            "category",
            "reorderLevel"
    );

    public void validate(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid sort field: " + order.getProperty()
                );
            }
        }
    }
}