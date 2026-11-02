package com.anshtechsolution.smart_inventory.sales;

import com.anshtechsolution.smart_inventory.exception.ProductNotFoundException;
import com.anshtechsolution.smart_inventory.inventory.StockMovement;
import com.anshtechsolution.smart_inventory.inventory.StockMovementRepository;
import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.product.ProductRepository;
import com.anshtechsolution.smart_inventory.sales.dto.CreateSaleRequest;
import com.anshtechsolution.smart_inventory.sales.dto.SaleItemRequest;
import com.anshtechsolution.smart_inventory.sales.dto.SaleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @InjectMocks
    private SalesService salesService;

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
    void createSale_shouldCreateSaleAndDeductStock() {

        // Arrange
        SaleItemRequest item =
                new SaleItemRequest(1L, 2);

        CreateSaleRequest request =
                new CreateSaleRequest("Rahul", List.of(item));

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> {
                    Sale sale = invocation.getArgument(0);
                    sale.setId(101L);
                    return sale;
                });

        // Act
        SaleResponse response = salesService.createSale(request);

        // Assert
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getCustomerName()).isEqualTo("Rahul");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");

        assertThat(response.getTotalAmount())
                .isEqualByComparingTo("100000.00");

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity())
                .isEqualTo(2);

        assertThat(product.getQuantity()).isEqualTo(98);

        verify(saleRepository).save(any(Sale.class));
        verify(productRepository).saveAll(anyCollection());
        verify(movementRepository).saveAll(anyCollection());
    }

    @Test
    void createSale_shouldCalculateTotalForMultipleItems() {

        // Arrange
        Product secondProduct = Product.builder()
                .id(2L)
                .sku("MOUSE-001")
                .name("Wireless Mouse")
                .quantity(50)
                .reorderLevel(5)
                .unitPrice(new BigDecimal("1000.00"))
                .build();

        CreateSaleRequest request =
                new CreateSaleRequest(
                        "Amit",
                        List.of(
                                new SaleItemRequest(1L, 2),
                                new SaleItemRequest(2L, 3)
                        )
                );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(secondProduct));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> {
                    Sale sale = invocation.getArgument(0);
                    sale.setId(102L);
                    return sale;
                });

        // Act
        SaleResponse response = salesService.createSale(request);

        // Assert
        // 2 x 50000 + 3 x 1000 = 103000
        assertThat(response.getTotalAmount())
                .isEqualByComparingTo("103000.00");

        assertThat(response.getItems()).hasSize(2);

        assertThat(product.getQuantity()).isEqualTo(98);
        assertThat(secondProduct.getQuantity()).isEqualTo(47);

        verify(productRepository).saveAll(anyCollection());
        verify(movementRepository).saveAll(anyCollection());
    }

    @Test
    void createSale_shouldRejectDuplicateProductIds() {

        // Arrange
        CreateSaleRequest request =
                new CreateSaleRequest(
                        "Rahul",
                        List.of(
                                new SaleItemRequest(1L, 2),
                                new SaleItemRequest(1L, 3)
                        )
                );

        // Act & Assert
        assertThatThrownBy(() ->
                salesService.createSale(request)
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex =
                            (ResponseStatusException) exception;

                    assertThat(ex.getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(productRepository, never())
                .findByIdForUpdate(anyLong());

        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void createSale_shouldRejectInsufficientStock() {

        // Arrange
        CreateSaleRequest request =
                new CreateSaleRequest(
                        "Rahul",
                        List.of(new SaleItemRequest(1L, 150))
                );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() ->
                salesService.createSale(request)
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex =
                            (ResponseStatusException) exception;

                    assertThat(ex.getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT);
                });

        assertThat(product.getQuantity()).isEqualTo(100);

        verify(saleRepository, never()).save(any(Sale.class));
        verify(productRepository, never()).saveAll(anyCollection());
        verify(movementRepository, never()).saveAll(anyCollection());
    }

    @Test
    void createSale_shouldThrowExceptionWhenProductDoesNotExist() {

        // Arrange
        CreateSaleRequest request =
                new CreateSaleRequest(
                        "Rahul",
                        List.of(new SaleItemRequest(99L, 2))
                );

        when(productRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                salesService.createSale(request)
        )
                .isInstanceOf(ProductNotFoundException.class);

        verify(saleRepository, never()).save(any(Sale.class));
        verify(productRepository, never()).saveAll(anyCollection());
        verify(movementRepository, never()).saveAll(anyCollection());
    }

    @Test
    void createSale_shouldGenerateStockOutMovements() {

        // Arrange
        CreateSaleRequest request =
                new CreateSaleRequest(
                        "Rahul",
                        List.of(new SaleItemRequest(1L, 5))
                );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        salesService.createSale(request);

        // Assert
        verify(movementRepository).saveAll(
                argThat(movements -> {
                    List<StockMovement> movementList =
                            (List<StockMovement>) movements;

                    return movementList.size() == 1
                            && movementList.get(0)
                            .getMovementType().equals("OUT")
                            && movementList.get(0)
                            .getQuantity().equals(5)
                            && movementList.get(0)
                            .getReference().startsWith("SALE-");
                })
        );

        assertThat(product.getQuantity()).isEqualTo(95);
    }

    @Test
    void getSale_shouldReturnSaleWhenSaleExists() {

        // Arrange
        Sale sale = Sale.builder()
                .id(101L)
                .saleNumber("SALE-TEST-001")
                .customerName("Rahul")
                .totalAmount(new BigDecimal("100000.00"))
                .status("COMPLETED")
                .build();

        when(saleRepository.findById(101L))
                .thenReturn(Optional.of(sale));

        // Act
        SaleResponse response = salesService.getSale(101L);

        // Assert
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getSaleNumber())
                .isEqualTo("SALE-TEST-001");

        assertThat(response.getCustomerName()).isEqualTo("Rahul");
        assertThat(response.getTotalAmount())
                .isEqualByComparingTo("100000.00");

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void getSale_shouldThrowExceptionWhenSaleDoesNotExist() {

        // Arrange
        when(saleRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> salesService.getSale(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex =
                            (ResponseStatusException) exception;

                    assertThat(ex.getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND);
                });
    }
}