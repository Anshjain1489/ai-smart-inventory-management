package com.anshtechsolution.smart_inventory.inventory;

import com.anshtechsolution.smart_inventory.exception.ProductNotFoundException;
import com.anshtechsolution.smart_inventory.inventory.dto.*;
import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    @Transactional
    public StockMovementResponse stockIn(
            Long productId,
            StockAdjustmentRequest request,
            String idempotencyKey) {

        validateIdempotencyKey(idempotencyKey);

        Product product = findProductForUpdate(productId);

        StockMovement existing =
                movementRepository.findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existing != null) {
            validateRetry(existing, productId, "IN", request);
            return toResponse(existing, existing.getProduct());
        }

        long newQuantity =
                (long) product.getQuantity() + request.getQuantity();

        if (newQuantity > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock quantity exceeds the supported limit"
            );
        }

        product.setQuantity((int) newQuantity);
        productRepository.save(product);

        StockMovement movement =
                createMovement(product, "IN", request);

        movement.setIdempotencyKey(idempotencyKey);

        StockMovement saved = movementRepository.saveAndFlush(movement);

        return toResponse(saved, product);
    }

    @Transactional
    public StockMovementResponse stockOut(
            Long productId,
            StockAdjustmentRequest request,
            String idempotencyKey) {

        validateIdempotencyKey(idempotencyKey);

        Product product = findProductForUpdate(productId);

        StockMovement existing =
                movementRepository.findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existing != null) {
            validateRetry(existing, productId, "OUT", request);
            return toResponse(existing, existing.getProduct());
        }

        if (product.getQuantity() < request.getQuantity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock. Available quantity: "
                            + product.getQuantity()
            );
        }

        product.setQuantity(
                product.getQuantity() - request.getQuantity()
        );

        productRepository.save(product);

        StockMovement movement =
                createMovement(product, "OUT", request);

        movement.setIdempotencyKey(idempotencyKey);

        StockMovement saved = movementRepository.saveAndFlush(movement);

        return toResponse(saved, product);
    }

    public Page<Product> getLowStockProducts(Pageable pageable) {
        return productRepository.findLowStockProducts(pageable);
    }

    // Existing product-specific movement history
    public Page<StockMovementResponse> getMovementHistory(
            Long productId,
            Pageable pageable) {

        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        return movementRepository
                .findByProduct_IdOrderByCreatedAtDesc(productId, pageable)
                .map(movement ->
                        toResponse(movement, movement.getProduct()));
    }

    // All products' movement history
    public Page<StockMovementResponse> getAllMovementHistory(
            Pageable pageable) {

        return movementRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(movement ->
                        toResponse(movement, movement.getProduct()));
    }

    // All products' movement history filtered by IN or OUT
    public Page<StockMovementResponse> getMovementHistoryByType(
            String movementType,
            Pageable pageable) {

        String validatedType = validateMovementType(movementType);

        return movementRepository
                .findByMovementTypeOrderByCreatedAtDesc(
                        validatedType,
                        pageable
                )
                .map(movement ->
                        toResponse(movement, movement.getProduct()));
    }

    // Specific product's movement history filtered by IN or OUT
    public Page<StockMovementResponse> getProductMovementHistoryByType(
            Long productId,
            String movementType,
            Pageable pageable) {

        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        String validatedType = validateMovementType(movementType);

        return movementRepository
                .findByProduct_IdAndMovementTypeOrderByCreatedAtDesc(
                        productId,
                        validatedType,
                        pageable
                )
                .map(movement ->
                        toResponse(movement, movement.getProduct()));
    }

    private String validateMovementType(String movementType) {

        if (movementType == null || movementType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Movement type is required. Use IN or OUT."
            );
        }

        String type = movementType.trim().toUpperCase();

        if (!type.equals("IN") && !type.equals("OUT")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid movement type. Allowed values: IN, OUT."
            );
        }

        return type;
    }

    private Product findProductForUpdate(Long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(
                        () -> new ProductNotFoundException(productId));
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain 1 to 100 characters"
            );
        }
    }

    private void validateRetry(
            StockMovement existing,
            Long productId,
            String movementType,
            StockAdjustmentRequest request) {

        boolean sameOperation =
                existing.getProduct().getId().equals(productId)
                        && existing.getMovementType().equals(movementType)
                        && existing.getQuantity().equals(request.getQuantity())
                        && java.util.Objects.equals(
                        existing.getReference(),
                        request.getReference())
                        && java.util.Objects.equals(
                        existing.getNotes(),
                        request.getNotes());

        if (!sameOperation) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency-Key has already been used "
                            + "for a different operation"
            );
        }
    }

    private StockMovement createMovement(
            Product product,
            String movementType,
            StockAdjustmentRequest request) {

        return StockMovement.builder()
                .product(product)
                .movementType(movementType)
                .quantity(request.getQuantity())
                .reference(request.getReference())
                .notes(request.getNotes())
                .build();
    }

    private StockMovementResponse toResponse(
            StockMovement movement,
            Product product) {

        return new StockMovementResponse(
                movement.getId(),
                product.getId(),
                product.getName(),
                movement.getMovementType(),
                movement.getQuantity(),
                product.getQuantity(),
                movement.getReference(),
                movement.getNotes(),
                movement.getCreatedAt()
        );
    }

    public InventoryAnalyticsResponse getInventoryAnalytics() {

        Long totalStockIn = movementRepository.getTotalStockIn();

        Long totalStockOut = movementRepository.getTotalStockOut();

        Long totalInTransactions =
                movementRepository.countByMovementType("IN");

        Long totalOutTransactions =
                movementRepository.countByMovementType("OUT");

        Long netStockMovement = totalStockIn - totalStockOut;

        return InventoryAnalyticsResponse.builder()
                .totalStockIn(totalStockIn)
                .totalStockOut(totalStockOut)
                .netStockMovement(netStockMovement)
                .totalInTransactions(totalInTransactions)
                .totalOutTransactions(totalOutTransactions)
                .build();
    }

    public InventoryAnalyticsResponse getInventoryAnalyticsByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date cannot be after end date"
            );
        }

        Long totalStockIn =
                movementRepository.getTotalStockInBetween(
                        startDate,
                        endDate
                );

        Long totalStockOut =
                movementRepository.getTotalStockOutBetween(
                        startDate,
                        endDate
                );

        Long totalInTransactions =
                movementRepository.countByMovementTypeAndCreatedAtBetween(
                        "IN",
                        startDate,
                        endDate
                );

        Long totalOutTransactions =
                movementRepository.countByMovementTypeAndCreatedAtBetween(
                        "OUT",
                        startDate,
                        endDate
                );

        return InventoryAnalyticsResponse.builder()
                .totalStockIn(totalStockIn)
                .totalStockOut(totalStockOut)
                .netStockMovement(totalStockIn - totalStockOut)
                .totalInTransactions(totalInTransactions)
                .totalOutTransactions(totalOutTransactions)
                .build();
    }

    public ProductInventoryAnalyticsResponse getProductInventoryAnalytics(Long productId) {

        // Verify that the product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // Calculate stock movement totals
        Long totalStockIn =
                movementRepository.getTotalStockInByProductId(productId);

        Long totalStockOut =
                movementRepository.getTotalStockOutByProductId(productId);

        // Calculate transaction counts
        Long totalInTransactions =
                movementRepository.countByProduct_IdAndMovementType(productId, "IN");

        Long totalOutTransactions =
                movementRepository.countByProduct_IdAndMovementType(productId, "OUT");

        // Calculate net stock movement
        Long netStockMovement = totalStockIn - totalStockOut;

        return ProductInventoryAnalyticsResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .totalStockIn(totalStockIn)
                .totalStockOut(totalStockOut)
                .netStockMovement(netStockMovement)
                .totalInTransactions(totalInTransactions)
                .totalOutTransactions(totalOutTransactions)
                .build();
    }

    public CategoryInventoryAnalyticsResponse getCategoryInventoryAnalytics(
            String category) {

        // Validate category
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category is required"
            );
        }

        String normalizedCategory = category.trim();

        // Check whether the category exists
        Long totalProducts =
                productRepository.countByCategoryIgnoreCase(normalizedCategory);

        if (totalProducts == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Category not found: " + normalizedCategory
            );
        }

        // Calculate stock movement
        Long totalStockIn =
                movementRepository.getTotalStockInByCategory(normalizedCategory);

        Long totalStockOut =
                movementRepository.getTotalStockOutByCategory(normalizedCategory);

        // Count transactions
        Long totalInTransactions =
                movementRepository.countByProduct_CategoryAndMovementType(
                        normalizedCategory, "IN");

        Long totalOutTransactions =
                movementRepository.countByProduct_CategoryAndMovementType(
                        normalizedCategory, "OUT");

        return CategoryInventoryAnalyticsResponse.builder()
                .categoryName(normalizedCategory)
                .totalProducts(totalProducts)
                .totalStockIn(totalStockIn)
                .totalStockOut(totalStockOut)
                .netStockMovement(totalStockIn - totalStockOut)
                .totalInTransactions(totalInTransactions)
                .totalOutTransactions(totalOutTransactions)
                .build();
    }

    public SupplierInventoryAnalyticsResponse getSupplierInventoryAnalytics(
            Long supplierId) {

        // Validate supplier ID
        if (supplierId == null || supplierId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid supplier ID is required"
            );
        }

        // Count products belonging to this supplier
        Long totalProducts = productRepository.countBySupplierId(supplierId);

        // Check whether supplier has products
        if (totalProducts == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No products found for supplier ID: " + supplierId
            );
        }

        // Calculate stock movement totals
        Long totalStockIn =
                movementRepository.getTotalStockInBySupplierId(supplierId);

        Long totalStockOut =
                movementRepository.getTotalStockOutBySupplierId(supplierId);

        // Count transactions
        Long totalInTransactions =
                movementRepository.countByProduct_SupplierIdAndMovementType(
                        supplierId, "IN");

        Long totalOutTransactions =
                movementRepository.countByProduct_SupplierIdAndMovementType(
                        supplierId, "OUT");

        // Calculate net movement
        Long netStockMovement = totalStockIn - totalStockOut;

        return SupplierInventoryAnalyticsResponse.builder()
                .supplierId(supplierId)
                .totalProducts(totalProducts)
                .totalStockIn(totalStockIn)
                .totalStockOut(totalStockOut)
                .netStockMovement(netStockMovement)
                .totalInTransactions(totalInTransactions)
                .totalOutTransactions(totalOutTransactions)
                .build();
    }

    public InventoryDashboardResponse getInventoryDashboard() {

        // Product statistics
        Long totalProducts = productRepository.countTotalProducts();

        Long totalInventoryQuantity =
                productRepository.getTotalInventoryQuantity();

        Long lowStockProducts =
                productRepository.countLowStockProducts();

        // Stock movement statistics
        Long totalStockIn = movementRepository.getTotalStockIn();

        Long totalStockOut = movementRepository.getTotalStockOut();

        Long totalInTransactions =
                movementRepository.countByMovementType("IN");

        Long totalOutTransactions =
                movementRepository.countByMovementType("OUT");

        // Calculate net stock movement
        Long netStockMovement = totalStockIn - totalStockOut;

        return InventoryDashboardResponse.builder()
                .totalProducts(totalProducts)
                .totalInventoryQuantity(totalInventoryQuantity)
                .lowStockProducts(lowStockProducts)
                .totalStockIn(totalStockIn)
                .totalStockOut(totalStockOut)
                .netStockMovement(netStockMovement)
                .totalInTransactions(totalInTransactions)
                .totalOutTransactions(totalOutTransactions)
                .build();
    }
}