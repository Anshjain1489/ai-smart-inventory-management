package com.anshtechsolution.smart_inventory.product;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProductPaginationValidator {

    private static final int MAX_PAGE_SIZE = 100;

    public void validate(Pageable pageable) {

        if (pageable.getPageNumber() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (pageable.getPageSize() < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be at least 1"
            );
        }

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size cannot exceed " + MAX_PAGE_SIZE
            );
        }
    }
}



