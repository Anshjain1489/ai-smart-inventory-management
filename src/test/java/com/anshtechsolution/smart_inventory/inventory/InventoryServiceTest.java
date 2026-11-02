package com.anshtechsolution.smart_inventory.inventory;

import com.anshtechsolution.smart_inventory.inventory.dto.StockAdjustmentRequest;
import com.anshtechsolution.smart_inventory.inventory.dto.StockMovementResponse;
import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("LAPTOP-001")
                .name("Dell Laptop")
                .category("Electronics")
                .quantity(100)
                .reorderLevel(10)
                .unitPrice(new BigDecimal("50000.00"))
                .build();
    }

    @Test
    void stockIn_shouldIncreaseProductQuantity() {

        // Arrange
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(
                        20,
                        "PURCHASE-001",
                        "New stock received"
                );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(movementRepository.findByIdempotencyKey("stock-in-001"))
                .thenReturn(Optional.empty());

        when(movementRepository.saveAndFlush(any(StockMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        StockMovementResponse response =
                inventoryService.stockIn(1L, request, "stock-in-001");

        // Assert
        assertThat(product.getQuantity()).isEqualTo(120);
        assertThat(response.movementType()).isEqualTo("IN");
        assertThat(response.quantity()).isEqualTo(20);
        assertThat(response.currentStock()).isEqualTo(120);

        verify(productRepository).save(product);
        verify(movementRepository).saveAndFlush(any(StockMovement.class));
    }

    @Test
    void stockOut_shouldDecreaseProductQuantity() {

        // Arrange
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(
                        30,
                        "SALE-001",
                        "Customer purchase"
                );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(movementRepository.findByIdempotencyKey("stock-out-001"))
                .thenReturn(Optional.empty());

        when(movementRepository.saveAndFlush(any(StockMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        StockMovementResponse response =
                inventoryService.stockOut(1L, request, "stock-out-001");

        // Assert
        assertThat(product.getQuantity()).isEqualTo(70);
        assertThat(response.movementType()).isEqualTo("OUT");
        assertThat(response.quantity()).isEqualTo(30);
        assertThat(response.currentStock()).isEqualTo(70);

        verify(productRepository).save(product);
        verify(movementRepository).saveAndFlush(any(StockMovement.class));
    }

    @Test
    void stockOut_shouldThrowExceptionWhenStockIsInsufficient() {

        // Arrange
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(
                        150,
                        "SALE-002",
                        "Large order"
                );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(movementRepository.findByIdempotencyKey("stock-out-002"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                inventoryService.stockOut(1L, request, "stock-out-002")
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseException =
                            (ResponseStatusException) exception;

                    assertThat(responseException.getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT);
                });

        assertThat(product.getQuantity()).isEqualTo(100);

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never())
                .saveAndFlush(any(StockMovement.class));
    }

    @Test
    void stockIn_shouldThrowExceptionWhenProductDoesNotExist() {

        // Arrange
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(
                        10,
                        "PURCHASE-002",
                        "Stock received"
                );

        when(productRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                inventoryService.stockIn(99L, request, "stock-in-002")
        )
                .isInstanceOf(
                        com.anshtechsolution.smart_inventory.exception.ProductNotFoundException.class
                );

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never())
                .saveAndFlush(any(StockMovement.class));
    }

    @Test
    void stockIn_shouldNotCreateDuplicateMovementForSameIdempotencyKey() {

        // Arrange
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(
                        20,
                        "PURCHASE-003",
                        "Stock received"
                );

        StockMovement existingMovement = StockMovement.builder()
                .id(10L)
                .product(product)
                .movementType("IN")
                .quantity(20)
                .reference("PURCHASE-003")
                .notes("Stock received")
                .idempotencyKey("stock-in-003")
                .build();

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(movementRepository.findByIdempotencyKey("stock-in-003"))
                .thenReturn(Optional.of(existingMovement));

        // Act
        StockMovementResponse response =
                inventoryService.stockIn(1L, request, "stock-in-003");

        // Assert
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.movementType()).isEqualTo("IN");

        // Quantity must remain unchanged on a retry.
        assertThat(product.getQuantity()).isEqualTo(100);

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never())
                .saveAndFlush(any(StockMovement.class));
    }

    @Test
    void stockIn_shouldRejectIdempotencyKeyUsedForDifferentOperation() {

        // Arrange
        StockAdjustmentRequest request =
                new StockAdjustmentRequest(
                        50,
                        "PURCHASE-004",
                        "Different stock quantity"
                );

        StockMovement existingMovement = StockMovement.builder()
                .id(11L)
                .product(product)
                .movementType("IN")
                .quantity(20)
                .reference("PURCHASE-004")
                .notes("Different stock quantity")
                .idempotencyKey("stock-in-004")
                .build();

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(movementRepository.findByIdempotencyKey("stock-in-004"))
                .thenReturn(Optional.of(existingMovement));

        // Act & Assert
        assertThatThrownBy(() ->
                inventoryService.stockIn(1L, request, "stock-in-004")
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseException =
                            (ResponseStatusException) exception;

                    assertThat(responseException.getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT);
                });

        assertThat(product.getQuantity()).isEqualTo(100);

        verify(productRepository, never()).save(any(Product.class));
        verify(movementRepository, never())
                .saveAndFlush(any(StockMovement.class));
    }
}