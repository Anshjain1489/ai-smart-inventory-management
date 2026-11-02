package com.anshtechsolution.smart_inventory.purchaseorder;

import com.anshtechsolution.smart_inventory.inventory.StockMovement;
import com.anshtechsolution.smart_inventory.inventory.StockMovementRepository;
import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.product.ProductRepository;
import com.anshtechsolution.smart_inventory.purchaseorder.dto.*;
import com.anshtechsolution.smart_inventory.supplier.Supplier;
import com.anshtechsolution.smart_inventory.supplier.SupplierRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Supplier supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        supplier = mock(Supplier.class);

        // Supplier is used when mapping purchase order responses.
        // Lenient avoids errors in tests that do not need these values.
        lenient().when(supplier.getId()).thenReturn(1L);
        lenient().when(supplier.getName()).thenReturn("ABC Suppliers");

        // Use a REAL Product object instead of a Mockito mock.
        // Its quantity will change when setQuantity() is called.
        product = new Product();
        product.setId(10L);
        product.setName("Laptop");
        product.setQuantity(20);
    }

    // 1. Create purchase order successfully
    @Test
    void createPurchaseOrder_shouldCalculateTotalAmount() {

        PurchaseOrderRequestDTO request =
                new PurchaseOrderRequestDTO(
                        1L,
                        LocalDateTime.now().plusDays(7),
                        "Test order",
                        List.of(
                                new PurchaseOrderItemRequestDTO(
                                        10L, 5, new BigDecimal("100.00")
                                )
                        )
                );

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> {
                    PurchaseOrder order = invocation.getArgument(0);
                    if (order.getId() == null) {
                        order.setId(100L);
                    }
                    return order;
                });

        when(purchaseOrderItemRepository.save(any(PurchaseOrderItem.class)))
                .thenAnswer(invocation -> {
                    PurchaseOrderItem item = invocation.getArgument(0);
                    item.setId(200L);
                    return item;
                });

        when(purchaseOrderItemRepository.findByPurchaseOrder_Id(100L))
                .thenReturn(List.of());

        PurchaseOrderResponseDTO response =
                purchaseOrderService.createPurchaseOrder(request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.supplierId()).isEqualTo(1L);
        assertThat(response.supplierName()).isEqualTo("ABC Suppliers");
        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(response.totalAmount())
                .isEqualByComparingTo("500.00");

        verify(purchaseOrderRepository, times(2))
                .save(any(PurchaseOrder.class));

        verify(purchaseOrderItemRepository)
                .save(any(PurchaseOrderItem.class));
    }

    // 2. Supplier does not exist
    @Test
    void createPurchaseOrder_shouldThrowWhenSupplierNotFound() {

        PurchaseOrderRequestDTO request =
                new PurchaseOrderRequestDTO(
                        99L,
                        null,
                        null,
                        List.of(
                                new PurchaseOrderItemRequestDTO(
                                        10L, 2, new BigDecimal("100.00")
                                )
                        )
                );

        when(supplierRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                purchaseOrderService.createPurchaseOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found");

        verify(purchaseOrderRepository, never())
                .save(any(PurchaseOrder.class));
    }

    // 3. Product does not exist
    @Test
    void createPurchaseOrder_shouldThrowWhenProductNotFound() {

        PurchaseOrderRequestDTO request =
                new PurchaseOrderRequestDTO(
                        1L,
                        null,
                        null,
                        List.of(
                                new PurchaseOrderItemRequestDTO(
                                        99L, 2, new BigDecimal("100.00")
                                )
                        )
                );

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> {
                    PurchaseOrder order = invocation.getArgument(0);
                    order.setId(100L);
                    return order;
                });

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                purchaseOrderService.createPurchaseOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");

    }

    // 4. Get all purchase orders
    @Test
    void getAllPurchaseOrders_shouldReturnOrders() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.DRAFT);

        when(purchaseOrderRepository.findAll())
                .thenReturn(List.of(order));

        when(purchaseOrderItemRepository.findByPurchaseOrder_Id(100L))
                .thenReturn(List.of());

        List<PurchaseOrderResponseDTO> response =
                purchaseOrderService.getAllPurchaseOrders();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(100L);
        assertThat(response.get(0).status())
                .isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    // 5. Get purchase order by ID
    @Test
    void getPurchaseOrderById_shouldReturnOrder() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.ORDERED);

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderItemRepository.findByPurchaseOrder_Id(100L))
                .thenReturn(List.of());

        PurchaseOrderResponseDTO response =
                purchaseOrderService.getPurchaseOrderById(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status())
                .isEqualTo(PurchaseOrderStatus.ORDERED);
    }

    // 6. Get purchase order by missing ID
    @Test
    void getPurchaseOrderById_shouldThrowWhenNotFound() {

        when(purchaseOrderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                purchaseOrderService.getPurchaseOrderById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Purchase order not found");
    }

    // 7. Get purchase orders by supplier
    @Test
    void getPurchaseOrdersBySupplier_shouldReturnOrders() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.DRAFT);

        when(supplierRepository.existsById(1L)).thenReturn(true);
        when(purchaseOrderRepository.findBySupplier_Id(1L))
                .thenReturn(List.of(order));

        when(purchaseOrderItemRepository.findByPurchaseOrder_Id(100L))
                .thenReturn(List.of());

        List<PurchaseOrderResponseDTO> response =
                purchaseOrderService.getPurchaseOrdersBySupplier(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).supplierId()).isEqualTo(1L);
    }

    // 8. Supplier does not exist while filtering
    @Test
    void getPurchaseOrdersBySupplier_shouldThrowWhenSupplierNotFound() {

        when(supplierRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() ->
                purchaseOrderService.getPurchaseOrdersBySupplier(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Supplier not found");
    }

    // 9. Get purchase orders by status
    @Test
    void getPurchaseOrdersByStatus_shouldReturnMatchingOrders() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.DRAFT);

        when(purchaseOrderRepository.findByStatus(PurchaseOrderStatus.DRAFT))
                .thenReturn(List.of(order));

        when(purchaseOrderItemRepository.findByPurchaseOrder_Id(100L))
                .thenReturn(List.of());

        List<PurchaseOrderResponseDTO> response =
                purchaseOrderService.getPurchaseOrdersByStatus(
                        PurchaseOrderStatus.DRAFT
                );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).status())
                .isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    // 10. Place a DRAFT order
    @Test
    void placePurchaseOrder_shouldChangeStatusToOrdered() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.DRAFT);

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(purchaseOrderItemRepository.findByPurchaseOrder_Id(100L))
                .thenReturn(List.of());

        PurchaseOrderResponseDTO response =
                purchaseOrderService.placePurchaseOrder(100L);

        assertThat(response.status())
                .isEqualTo(PurchaseOrderStatus.ORDERED);

        verify(purchaseOrderRepository).save(order);
    }

    // 11. Cannot place a non-DRAFT order
    @Test
    void placePurchaseOrder_shouldRejectNonDraftOrder() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.ORDERED);

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                purchaseOrderService.placePurchaseOrder(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only DRAFT");

        verify(purchaseOrderRepository, never())
                .save(any(PurchaseOrder.class));
    }

    // 12. Receive partial stock
    @Test
    void receiveStock_shouldUpdateStockAndSetPartiallyReceived() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.ORDERED);
        PurchaseOrderItem item = createItem(order, product, 10, 0);

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderItemRepository.findById(200L))
                .thenReturn(Optional.of(item));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        when(purchaseOrderItemRepository.findByPurchaseOrder_Id(100L))
                .thenReturn(List.of(item));

        when(purchaseOrderItemRepository.save(any(PurchaseOrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(purchaseOrderItemRepository.findByPurchaseOrder_Id(100L))
                .thenReturn(List.of(item));

        PurchaseOrderResponseDTO response =
                purchaseOrderService.receiveStock(
                        100L, 200L, new ReceiveStockRequestDTO(4)
                );

        assertThat(item.getReceivedQuantity()).isEqualTo(4);
        assertThat(product.getQuantity()).isEqualTo(24);
        assertThat(response.status())
                .isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);

        verify(movementRepository).save(any(StockMovement.class));
    }

    // 13. Receive remaining stock and complete order
    @Test
    void receiveStock_shouldSetReceivedWhenAllItemsAreReceived() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.ORDERED);
        PurchaseOrderItem item = createItem(order, product, 10, 0);

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderItemRepository.findById(200L))
                .thenReturn(Optional.of(item));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        when(purchaseOrderItemRepository.findByPurchaseOrder_Id(100L))
                .thenReturn(List.of(item));

        when(purchaseOrderItemRepository.save(any(PurchaseOrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponseDTO response =
                purchaseOrderService.receiveStock(
                        100L, 200L, new ReceiveStockRequestDTO(10)
                );

        assertThat(item.getReceivedQuantity()).isEqualTo(10);
        assertThat(product.getQuantity()).isEqualTo(30);
        assertThat(response.status())
                .isEqualTo(PurchaseOrderStatus.RECEIVED);
    }

    // 14. Reject receiving zero quantity
    @Test
    void receiveStock_shouldRejectZeroQuantity() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.ORDERED);
        PurchaseOrderItem item = createItem(order, product, 10, 0);

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderItemRepository.findById(200L))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() ->
                purchaseOrderService.receiveStock(
                        100L, 200L, new ReceiveStockRequestDTO(0)
                ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception ->
                        assertThat(((ResponseStatusException) exception).getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST)
                );

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    // 15. Reject quantity greater than pending quantity
    @Test
    void receiveStock_shouldRejectQuantityGreaterThanPending() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.ORDERED);
        PurchaseOrderItem item = createItem(order, product, 10, 0);

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderItemRepository.findById(200L))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() ->
                purchaseOrderService.receiveStock(
                        100L, 200L, new ReceiveStockRequestDTO(15)
                ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception ->
                        assertThat(((ResponseStatusException) exception).getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT)
                );

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    // 16. Reject receiving stock for completed order
    @Test
    void receiveStock_shouldRejectCompletedOrder() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.RECEIVED);

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                purchaseOrderService.receiveStock(
                        100L, 200L, new ReceiveStockRequestDTO(2)
                ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception ->
                        assertThat(((ResponseStatusException) exception).getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT)
                );
    }

    // 17. Reject an item that belongs to another order
    @Test
    void receiveStock_shouldRejectItemFromAnotherOrder() {

        PurchaseOrder order = createOrder(100L, PurchaseOrderStatus.ORDERED);
        PurchaseOrder anotherOrder = createOrder(999L, PurchaseOrderStatus.ORDERED);

        PurchaseOrderItem item = createItem(anotherOrder, product, 10, 0);

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderItemRepository.findById(200L))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() ->
                purchaseOrderService.receiveStock(
                        100L, 200L, new ReceiveStockRequestDTO(2)
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong");

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    // 18. Reject stock overflow
    @Test
    void receiveStock_shouldRejectStockOverflow() {

        PurchaseOrder order = createOrder(
                100L,
                PurchaseOrderStatus.ORDERED
        );

        // Set the real product quantity to the maximum integer value.
        product.setQuantity(Integer.MAX_VALUE);

        PurchaseOrderItem item = createItem(
                order,
                product,
                10,
                0
        );

        when(purchaseOrderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderItemRepository.findById(200L))
                .thenReturn(Optional.of(item));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() ->
                purchaseOrderService.receiveStock(
                        100L,
                        200L,
                        new ReceiveStockRequestDTO(1)
                ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception ->
                        assertThat(
                                ((ResponseStatusException) exception).getStatusCode()
                        ).isEqualTo(HttpStatus.CONFLICT)
                );

        verify(productRepository, never())
                .save(any(Product.class));

        verify(movementRepository, never())
                .save(any(StockMovement.class));
    }

    // Helper: create a purchase order
    private PurchaseOrder createOrder(
            Long id,
            PurchaseOrderStatus status) {

        PurchaseOrder order = new PurchaseOrder();
        order.setId(id);
        order.setSupplier(supplier);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setNotes("Test order");

        return order;
    }

    // Helper: create a purchase order item
    private PurchaseOrderItem createItem(
            PurchaseOrder order,
            Product product,
            int orderedQuantity,
            int receivedQuantity) {

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(200L);
        item.setPurchaseOrder(order);
        item.setProduct(product);
        item.setOrderedQuantity(orderedQuantity);
        item.setReceivedQuantity(receivedQuantity);
        item.setUnitPrice(new BigDecimal("100.00"));

        return item;
    }
}