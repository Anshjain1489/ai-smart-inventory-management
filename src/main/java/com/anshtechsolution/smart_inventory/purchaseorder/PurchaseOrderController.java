package com.anshtechsolution.smart_inventory.purchaseorder;

import com.anshtechsolution.smart_inventory.purchaseorder.dto.PurchaseOrderRequestDTO;
import com.anshtechsolution.smart_inventory.purchaseorder.dto.PurchaseOrderResponseDTO;
import com.anshtechsolution.smart_inventory.purchaseorder.dto.ReceiveStockRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    // Create a purchase order
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponseDTO createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequestDTO request) {

        return purchaseOrderService.createPurchaseOrder(request);
    }

    // Get all purchase orders
    @GetMapping
    public List<PurchaseOrderResponseDTO> getAllPurchaseOrders() {
        return purchaseOrderService.getAllPurchaseOrders();
    }

    // Get a purchase order by ID
    @GetMapping("/{id}")
    public PurchaseOrderResponseDTO getPurchaseOrderById(
            @PathVariable Long id) {

        return purchaseOrderService.getPurchaseOrderById(id);
    }

    // Get purchase orders by supplier
    @GetMapping("/supplier/{supplierId}")
    public List<PurchaseOrderResponseDTO> getPurchaseOrdersBySupplier(
            @PathVariable Long supplierId) {

        return purchaseOrderService.getPurchaseOrdersBySupplier(supplierId);
    }

    // Get purchase orders by status
    @GetMapping("/status/{status}")
    public List<PurchaseOrderResponseDTO> getPurchaseOrdersByStatus(
            @PathVariable PurchaseOrderStatus status) {

        return purchaseOrderService.getPurchaseOrdersByStatus(status);
    }

    @PatchMapping("/{id}/order")
    public PurchaseOrderResponseDTO placePurchaseOrder(
            @PathVariable Long id) {

        return purchaseOrderService.placePurchaseOrder(id);
    }

    @PostMapping("/{purchaseOrderId}/items/{itemId}/receive")
    public PurchaseOrderResponseDTO receiveStock(
            @PathVariable Long purchaseOrderId,
            @PathVariable Long itemId,
            @Valid @RequestBody ReceiveStockRequestDTO request) {

        return purchaseOrderService.receiveStock(
                purchaseOrderId,
                itemId,
                request
        );
    }
}