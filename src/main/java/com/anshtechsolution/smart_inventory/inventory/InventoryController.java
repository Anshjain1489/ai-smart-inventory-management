package com.anshtechsolution.smart_inventory.inventory;

import com.anshtechsolution.smart_inventory.inventory.dto.*;
import com.anshtechsolution.smart_inventory.product.Product;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/{productId}/stock-in")
    public ResponseEntity<StockMovementResponse> stockIn(
            @PathVariable Long productId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody StockAdjustmentRequest request) {

        return ResponseEntity.ok(
                inventoryService.stockIn(productId, request, idempotencyKey)
        );
    }

    @PostMapping("/{productId}/stock-out")
    public ResponseEntity<StockMovementResponse> stockOut(
            @PathVariable Long productId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody StockAdjustmentRequest request) {

        return ResponseEntity.ok(
                inventoryService.stockOut(productId, request, idempotencyKey)
        );
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Page<Product>> getLowStockProducts(
            @PageableDefault(
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                inventoryService.getLowStockProducts(pageable)
        );
    }

    // Product-specific movement history
    @GetMapping("/{productId}/movements")
    public ResponseEntity<Page<StockMovementResponse>> getMovementHistory(
            @PathVariable Long productId,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                inventoryService.getMovementHistory(productId, pageable)
        );
    }

    // All products' movement history
    @GetMapping("/movements")
    public ResponseEntity<Page<StockMovementResponse>> getAllMovementHistory(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                inventoryService.getAllMovementHistory(pageable)
        );
    }

    // Filter all movements by IN or OUT
    @GetMapping("/movements/type/{movementType}")
    public ResponseEntity<Page<StockMovementResponse>> getMovementHistoryByType(
            @PathVariable String movementType,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                inventoryService.getMovementHistoryByType(
                        movementType,
                        pageable
                )
        );
    }

    // Filter a specific product's movements by IN or OUT
    @GetMapping("/{productId}/movements/type/{movementType}")
    public ResponseEntity<Page<StockMovementResponse>>
    getProductMovementHistoryByType(
            @PathVariable Long productId,
            @PathVariable String movementType,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                inventoryService.getProductMovementHistoryByType(
                        productId,
                        movementType,
                        pageable
                )
        );
    }

    @GetMapping("/analytics")
    public ResponseEntity<InventoryAnalyticsResponse> getInventoryAnalytics() {

        return ResponseEntity.ok(
                inventoryService.getInventoryAnalytics()
        );
    }

    @GetMapping("/analytics/date-range")
    public ResponseEntity<InventoryAnalyticsResponse> getAnalyticsByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate) {

        return ResponseEntity.ok(
                inventoryService.getInventoryAnalyticsByDateRange(
                        startDate,
                        endDate
                )
        );
    }

    @GetMapping("/analytics/product/{productId}")
    public ResponseEntity<ProductInventoryAnalyticsResponse>
    getProductInventoryAnalytics(@PathVariable Long productId) {

        return ResponseEntity.ok(
                inventoryService.getProductInventoryAnalytics(productId)
        );
    }

    @GetMapping("/analytics/category/{category}")
    public ResponseEntity<CategoryInventoryAnalyticsResponse>
    getCategoryInventoryAnalytics(@PathVariable String category) {

        return ResponseEntity.ok(
                inventoryService.getCategoryInventoryAnalytics(category)
        );
    }

    @GetMapping("/analytics/supplier/{supplierId}")
    public ResponseEntity<SupplierInventoryAnalyticsResponse>
    getSupplierInventoryAnalytics(@PathVariable Long supplierId) {

        return ResponseEntity.ok(
                inventoryService.getSupplierInventoryAnalytics(supplierId)
        );
    }

    @GetMapping("/dashboard")
    public ResponseEntity<InventoryDashboardResponse> getInventoryDashboard() {

        return ResponseEntity.ok(
                inventoryService.getInventoryDashboard()
        );
    }
}