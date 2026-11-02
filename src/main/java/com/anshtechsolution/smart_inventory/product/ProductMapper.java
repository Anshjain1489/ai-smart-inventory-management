package com.anshtechsolution.smart_inventory.product;

import com.anshtechsolution.smart_inventory.product.dto.ProductCreateRequest;
import com.anshtechsolution.smart_inventory.product.dto.ProductResponse;
import com.anshtechsolution.smart_inventory.product.dto.ProductUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductCreateRequest request) {
        return Product.builder()
                .sku(request.getSku().trim())
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .supplierId(request.getSupplierId())
                .quantity(request.getQuantity())
                .reorderLevel(request.getReorderLevel())
                .unitPrice(request.getUnitPrice())
                .build();
    }

    public void updateEntity(
            Product product,
            ProductUpdateRequest request) {

        product.setSku(request.getSku().trim());
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setSupplierId(request.getSupplierId());
        product.setQuantity(request.getQuantity());
        product.setReorderLevel(request.getReorderLevel());
        product.setUnitPrice(request.getUnitPrice());
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .supplierId(product.getSupplierId())
                .quantity(product.getQuantity())
                .reorderLevel(product.getReorderLevel())
                .unitPrice(product.getUnitPrice())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
