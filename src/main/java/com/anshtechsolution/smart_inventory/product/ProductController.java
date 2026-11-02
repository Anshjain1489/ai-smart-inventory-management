package com.anshtechsolution.smart_inventory.product;

import com.anshtechsolution.smart_inventory.product.dto.ProductCreateRequest;
import com.anshtechsolution.smart_inventory.product.dto.ProductResponse;
import com.anshtechsolution.smart_inventory.product.dto.ProductUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @Valid @RequestBody ProductCreateRequest request) {
        return productService.create(request);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping
    public Page<ProductResponse> getAll(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 10, sort = "id")
            Pageable pageable) {

        if (category == null || category.isBlank()) {
            return productService.getAll(pageable);
        }

        return productService.getByCategory(category, pageable);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    @GetMapping("/search")
    public Page<ProductResponse> searchProducts(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        return productService.searchByKeyword(keyword, pageable);
    }

    @GetMapping("/search/filter")
    public Page<ProductResponse> searchByKeywordAndCategory(
            @RequestParam String keyword,
            @RequestParam String category,
            @PageableDefault(size = 10, sort = "id")
            Pageable pageable) {

        return productService.searchByKeywordAndCategory(
                keyword, category, pageable);
    }

    @GetMapping("/stock-status")
    public Page<ProductResponse> getProductsByStockStatus(
            @RequestParam String status,
            @PageableDefault(size = 10, sort = "id")
            Pageable pageable) {

        return productService.getProductsByStockStatus(
                status, pageable);
    }

    @GetMapping("/filter")
    public Page<ProductResponse> filterProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "id")
            Pageable pageable) {

        return productService.filterProducts(
                keyword, category, status, pageable);
    }
}
