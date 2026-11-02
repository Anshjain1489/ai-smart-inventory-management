package com.anshtechsolution.smart_inventory.product;

import com.anshtechsolution.smart_inventory.exception.ProductNotFoundException;
import com.anshtechsolution.smart_inventory.product.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductPaginationValidator productPaginationValidator;

    @Mock
    private ProductSortValidator productSortValidator;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductResponse productResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("LAPTOP-001")
                .name("Dell Laptop")
                .description("Business Laptop")
                .category("Electronics")
                .quantity(100)
                .reorderLevel(10)
                .unitPrice(new BigDecimal("50000.00"))
                .build();

        productResponse = mock(ProductResponse.class);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getById_shouldReturnProductWhenProductExists() {

        // Arrange
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productMapper.toResponse(product))
                .thenReturn(productResponse);

        // Act
        ProductResponse response = productService.getById(1L);

        // Assert
        assertThat(response).isSameAs(productResponse);

        verify(productRepository).findById(1L);
        verify(productMapper).toResponse(product);
    }

    @Test
    void getById_shouldThrowExceptionWhenProductDoesNotExist() {

        // Arrange
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productMapper, never()).toResponse(any());
    }

    @Test
    void delete_shouldDeleteProductWhenProductExists() {

        // Arrange
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // Act
        productService.delete(1L);

        // Assert
        verify(productRepository).delete(product);
    }

    @Test
    void delete_shouldThrowExceptionWhenProductDoesNotExist() {

        // Arrange
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void getAll_shouldReturnAllProducts() {

        // Arrange
        Page<Product> productPage =
                new PageImpl<>(List.of(product));

        when(productRepository.findAll(pageable))
                .thenReturn(productPage);

        when(productMapper.toResponse(product))
                .thenReturn(productResponse);

        // Act
        Page<ProductResponse> response =
                productService.getAll(pageable);

        // Assert
        assertThat(response.getContent())
                .containsExactly(productResponse);

        assertThat(response.getTotalElements()).isEqualTo(1);

        verify(productPaginationValidator).validate(pageable);
        verify(productSortValidator).validate(pageable);
    }

    @Test
    void searchByKeyword_shouldTrimKeywordAndReturnResults() {

        // Arrange
        Page<Product> productPage =
                new PageImpl<>(List.of(product));

        when(productRepository.searchByKeyword("Laptop", pageable))
                .thenReturn(productPage);

        when(productMapper.toResponse(product))
                .thenReturn(productResponse);

        // Act
        Page<ProductResponse> response =
                productService.searchByKeyword(" Laptop ", pageable);

        // Assert
        assertThat(response.getContent())
                .containsExactly(productResponse);

        verify(productRepository)
                .searchByKeyword("Laptop", pageable);
    }

    @Test
    void searchByKeyword_shouldReturnAllProductsWhenKeywordIsBlank() {

        // Arrange
        Page<Product> productPage =
                new PageImpl<>(List.of(product));

        when(productRepository.findAll(pageable))
                .thenReturn(productPage);

        when(productMapper.toResponse(product))
                .thenReturn(productResponse);

        // Act
        Page<ProductResponse> response =
                productService.searchByKeyword("   ", pageable);

        // Assert
        assertThat(response.getContent())
                .containsExactly(productResponse);

        verify(productRepository).findAll(pageable);
        verify(productRepository, never())
                .searchByKeyword(anyString(), any(Pageable.class));
    }

    @Test
    void getByCategory_shouldTrimCategoryAndReturnResults() {

        // Arrange
        Page<Product> productPage =
                new PageImpl<>(List.of(product));

        when(productRepository.findByCategoryIgnoreCase(
                "Electronics", pageable))
                .thenReturn(productPage);

        when(productMapper.toResponse(product))
                .thenReturn(productResponse);

        // Act
        Page<ProductResponse> response =
                productService.getByCategory(" Electronics ", pageable);

        // Assert
        assertThat(response.getContent())
                .containsExactly(productResponse);

        verify(productRepository)
                .findByCategoryIgnoreCase("Electronics", pageable);
    }

    @Test
    void getProductsByStockStatus_shouldReturnOutOfStockProducts() {

        // Arrange
        Page<Product> productPage =
                new PageImpl<>(List.of(product));

        when(productRepository.findByQuantity(0, pageable))
                .thenReturn(productPage);

        when(productMapper.toResponse(product))
                .thenReturn(productResponse);

        // Act
        Page<ProductResponse> response =
                productService.getProductsByStockStatus(
                        "OUT_OF_STOCK", pageable);

        // Assert
        assertThat(response.getContent())
                .containsExactly(productResponse);

        verify(productRepository).findByQuantity(0, pageable);
    }

    @Test
    void getProductsByStockStatus_shouldReturnLowStockProducts() {

        // Arrange
        Page<Product> productPage =
                new PageImpl<>(List.of(product));

        when(productRepository.findProductsWithLowStock(pageable))
                .thenReturn(productPage);

        when(productMapper.toResponse(product))
                .thenReturn(productResponse);

        // Act
        Page<ProductResponse> response =
                productService.getProductsByStockStatus(
                        "LOW_STOCK", pageable);

        // Assert
        assertThat(response.getContent())
                .containsExactly(productResponse);

        verify(productRepository)
                .findProductsWithLowStock(pageable);
    }

    @Test
    void getProductsByStockStatus_shouldReturnInStockProducts() {

        // Arrange
        Page<Product> productPage =
                new PageImpl<>(List.of(product));

        when(productRepository.findProductsInStock(pageable))
                .thenReturn(productPage);

        when(productMapper.toResponse(product))
                .thenReturn(productResponse);

        // Act
        Page<ProductResponse> response =
                productService.getProductsByStockStatus(
                        "IN_STOCK", pageable);

        // Assert
        assertThat(response.getContent())
                .containsExactly(productResponse);

        verify(productRepository)
                .findProductsInStock(pageable);
    }

    @Test
    void getProductsByStockStatus_shouldRejectInvalidStatus() {

        // Act & Assert
        assertThatThrownBy(() ->
                productService.getProductsByStockStatus(
                        "INVALID", pageable)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid stock status");

        verify(productRepository, never())
                .findProductsInStock(any(Pageable.class));
    }

    @Test
    void filterProducts_shouldNormalizeFilterValues() {

        // Arrange
        Page<Product> productPage =
                new PageImpl<>(List.of(product));

        when(productRepository.filterProducts(
                "Laptop",
                "Electronics",
                "IN_STOCK",
                pageable
        )).thenReturn(productPage);

        when(productMapper.toResponse(product))
                .thenReturn(productResponse);

        // Act
        Page<ProductResponse> response =
                productService.filterProducts(
                        " Laptop ",
                        " Electronics ",
                        " in_stock ",
                        pageable
                );

        // Assert
        assertThat(response.getContent())
                .containsExactly(productResponse);

        verify(productRepository).filterProducts(
                "Laptop",
                "Electronics",
                "IN_STOCK",
                pageable
        );
    }

    @Test
    void filterProducts_shouldRejectInvalidStatus() {

        // Act & Assert
        assertThatThrownBy(() ->
                productService.filterProducts(
                        "Laptop",
                        "Electronics",
                        "INVALID",
                        pageable
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");

        verify(productRepository, never()).filterProducts(
                any(),
                any(),
                any(),
                any(Pageable.class)
        );
    }
}