package com.anshtechsolution.smart_inventory.purchaseorder;

import com.anshtechsolution.smart_inventory.inventory.StockMovement;
import com.anshtechsolution.smart_inventory.inventory.StockMovementRepository;
import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.product.ProductRepository;
import com.anshtechsolution.smart_inventory.purchaseorder.dto.*;
import com.anshtechsolution.smart_inventory.supplier.Supplier;
import com.anshtechsolution.smart_inventory.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    // Create a new purchase order
    public PurchaseOrderResponseDTO createPurchaseOrder(
            PurchaseOrderRequestDTO request) {

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new RuntimeException(
                        "Supplier not found with ID: " + request.supplierId()));

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setExpectedDeliveryDate(
                request.expectedDeliveryDate());
        purchaseOrder.setNotes(request.notes());
        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
        purchaseOrder.setTotalAmount(BigDecimal.ZERO);

        purchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemRequestDTO itemRequest : request.items()) {

            Product product = productRepository
                    .findById(itemRequest.productId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found with ID: "
                                    + itemRequest.productId()));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(purchaseOrder);
            item.setProduct(product);
            item.setOrderedQuantity(itemRequest.orderedQuantity());
            item.setReceivedQuantity(0);
            item.setUnitPrice(itemRequest.unitPrice());

            BigDecimal lineTotal = item.getLineTotal();
            totalAmount = totalAmount.add(lineTotal);

            purchaseOrderItemRepository.save(item);
        }

        purchaseOrder.setTotalAmount(totalAmount);
        purchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        return mapToResponse(purchaseOrder);
    }

    // Get all purchase orders
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponseDTO> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get a purchase order by ID
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDTO getPurchaseOrderById(Long id) {

        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase order not found with ID: " + id));

        return mapToResponse(order);
    }

    // Get purchase orders by supplier
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponseDTO> getPurchaseOrdersBySupplier(
            Long supplierId) {

        if (!supplierRepository.existsById(supplierId)) {
            throw new RuntimeException(
                    "Supplier not found with ID: " + supplierId);
        }

        return purchaseOrderRepository.findBySupplier_Id(supplierId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get purchase orders by status
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponseDTO> getPurchaseOrdersByStatus(
            PurchaseOrderStatus status) {

        return purchaseOrderRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Convert entity to response DTO
    private PurchaseOrderResponseDTO mapToResponse(
            PurchaseOrder order) {

        List<PurchaseOrderItemResponseDTO> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_Id(order.getId())
                        .stream()
                        .map(this::mapItemToResponse)
                        .toList();

        return new PurchaseOrderResponseDTO(
                order.getId(),
                order.getSupplier().getId(),
                order.getSupplier().getName(),
                order.getStatus(),
                order.getOrderDate(),
                order.getExpectedDeliveryDate(),
                order.getTotalAmount(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    // Convert item entity to response DTO
    private PurchaseOrderItemResponseDTO mapItemToResponse(
            PurchaseOrderItem item) {

        return new PurchaseOrderItemResponseDTO(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getOrderedQuantity(),
                item.getReceivedQuantity(),
                item.getPendingQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }

    // Place a purchase order
    public PurchaseOrderResponseDTO placePurchaseOrder(Long id) {

        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase order not found with ID: " + id));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new RuntimeException(
                    "Only DRAFT purchase orders can be placed.");
        }

        order.setStatus(PurchaseOrderStatus.ORDERED);

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        return mapToResponse(savedOrder);
    }

    // Receive stock and create stock movement audit record
    public PurchaseOrderResponseDTO receiveStock(
            Long purchaseOrderId,
            Long itemId,
            ReceiveStockRequestDTO request) {

        // 1. Find the purchase order.
        PurchaseOrder order = purchaseOrderRepository
                .findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase order not found with ID: " + purchaseOrderId));

        // 2. Validate purchase order status.
        if (order.getStatus() != PurchaseOrderStatus.ORDERED
                && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock cannot be received because this purchase order is already completed or cancelled."
            );
        }

        // 3. Find item and verify that it belongs to this order.
        PurchaseOrderItem item = purchaseOrderItemRepository
                .findById(itemId)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase order item not found with ID: " + itemId));

        if (!item.getPurchaseOrder().getId().equals(purchaseOrderId)) {
            throw new RuntimeException(
                    "The specified item does not belong to this purchase order.");
        }

        // 4. Validate received quantity.
        int receivedNow = request.receivedQuantity();
        int pending = item.getPendingQuantity();

        if (receivedNow <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Received quantity must be greater than zero."
            );
        }

        if (receivedNow > pending) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot receive " + receivedNow
                            + " units. Only " + pending + " units are pending."
            );
        }

        // 5. Find the product.
        Product product = productRepository
                .findById(item.getProduct().getId())
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with ID: " + item.getProduct().getId()));

        // 6. Update received quantity and inventory stock.
        long updatedStock = (long) product.getQuantity() + receivedNow;

        if (updatedStock > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock quantity exceeds the supported limit."
            );
        }

        item.setReceivedQuantity(
                item.getReceivedQuantity() + receivedNow);

        product.setQuantity((int) updatedStock);

        purchaseOrderItemRepository.save(item);
        productRepository.save(product);

        // 7. Create stock movement audit record.
        StockMovement movement = StockMovement.builder()
                .product(product)
                .movementType("IN")
                .quantity(receivedNow)
                .reference("PO-" + purchaseOrderId)
                .notes("Stock received against purchase order "
                        + purchaseOrderId
                        + ", item ID: " + itemId)
                .build();

        movementRepository.save(movement);

        // 8. Update purchase order status.
        List<PurchaseOrderItem> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrder_Id(purchaseOrderId);

        boolean allReceived = items.stream()
                .allMatch(i -> i.getReceivedQuantity()
                        .equals(i.getOrderedQuantity()));

        boolean anyReceived = items.stream()
                .anyMatch(i -> i.getReceivedQuantity() > 0);

        if (allReceived) {
            order.setStatus(PurchaseOrderStatus.RECEIVED);
        } else if (anyReceived) {
            order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        purchaseOrderRepository.save(order);

        // 9. Return updated purchase order.
        return mapToResponse(order);
    }
}