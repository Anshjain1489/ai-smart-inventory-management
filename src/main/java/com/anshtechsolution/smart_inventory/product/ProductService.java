package com.anshtechsolution.smart_inventory.product;

import com.anshtechsolution.smart_inventory.exception.ProductNotFoundException;
import com.anshtechsolution.smart_inventory.product.dto.ProductUpdateRequest;
import com.anshtechsolution.smart_inventory.product.dto.ProductCreateRequest;
import com.anshtechsolution.smart_inventory.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductPaginationValidator productPaginationValidator;
    private final ProductSortValidator productSortValidator;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        if (productRepository.existsBySku(request.getSku().trim())) {
            throw new ResponseStatusException(
                    CONFLICT, "A product with this SKU already exists");
        }

        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findProduct(id));
    }

    public Page<ProductResponse> getAll(Pageable pageable) {
        productPaginationValidator.validate(pageable);
        productSortValidator.validate(pageable);
        return productRepository.findAll(pageable)
                .map(productMapper::toResponse);
    }

    @Transactional
    public ProductResponse update(
            Long id,
            ProductUpdateRequest request) {

        Product product = findProduct(id);
        String sku = request.getSku().trim();

        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new ResponseStatusException(
                    CONFLICT, "Another product already uses this SKU");
        }

        productMapper.updateEntity(product, request);

        Product updatedProduct = productRepository.save(product);

        return productMapper.toResponse(updatedProduct);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        productRepository.delete(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Page<ProductResponse> searchByKeyword(
            String keyword,
            Pageable pageable) {

        productPaginationValidator.validate(pageable);
        productSortValidator.validate(pageable);

        productPaginationValidator.validate(pageable);
        productSortValidator.validate(pageable);

        if (keyword == null || keyword.isBlank()) {
            return getAll(pageable);
        }

        return productRepository
                .searchByKeyword(keyword.trim(), pageable)
                .map(productMapper::toResponse);
    }

    public Page<ProductResponse> getByCategory(
            String category,
            Pageable pageable) {

        productPaginationValidator.validate(pageable);
        productSortValidator.validate(pageable);

        if (category == null || category.isBlank()) {
            return getAll(pageable);
        }

        return productRepository
                .findByCategoryIgnoreCase(category.trim(), pageable)
                .map(productMapper::toResponse);
    }

    public Page<ProductResponse> searchByKeywordAndCategory(
            String keyword,
            String category,
            Pageable pageable) {

        productPaginationValidator.validate(pageable);
        productSortValidator.validate(pageable);

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(
                    "Keyword must not be blank");
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "Category must not be blank");
        }

        return productRepository.searchByKeywordAndCategory(
                        keyword.trim(),
                        category.trim(),
                        pageable)
                .map(productMapper::toResponse);
    }

    public Page<ProductResponse> getProductsByStockStatus(
            String status,
            Pageable pageable) {

        productPaginationValidator.validate(pageable);
        productSortValidator.validate(pageable);

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Stock status must not be blank");
        }

        return switch (status.trim().toUpperCase()) {
            case "OUT_OF_STOCK" ->
                    productRepository.findByQuantity(0, pageable)
                            .map(productMapper::toResponse);

            case "LOW_STOCK" ->
                    productRepository.findProductsWithLowStock(pageable)
                            .map(productMapper::toResponse);

            case "IN_STOCK" ->
                    productRepository.findProductsInStock(pageable)
                            .map(productMapper::toResponse);

            default -> throw new IllegalArgumentException(
                    "Invalid stock status. Use OUT_OF_STOCK, LOW_STOCK, or IN_STOCK");
        };
    }

    public Page<ProductResponse> filterProducts(
            String keyword,
            String category,
            String status,
            Pageable pageable) {

        productPaginationValidator.validate(pageable);
        productSortValidator.validate(pageable);

        String normalizedKeyword =
                (keyword == null || keyword.isBlank())
                        ? null : keyword.trim();

        String normalizedCategory =
                (category == null || category.isBlank())
                        ? null : category.trim();

        String normalizedStatus =
                (status == null || status.isBlank())
                        ? null : status.trim().toUpperCase();

        if (normalizedStatus != null
                && !normalizedStatus.equals("IN_STOCK")
                && !normalizedStatus.equals("LOW_STOCK")
                && !normalizedStatus.equals("OUT_OF_STOCK")) {
            throw new IllegalArgumentException(
                    "Invalid status. Use IN_STOCK, LOW_STOCK, or OUT_OF_STOCK");
        }

        return productRepository.filterProducts(
                        normalizedKeyword,
                        normalizedCategory,
                        normalizedStatus,
                        pageable)
                .map(productMapper::toResponse);
    }
}