package com.anshtechsolution.smart_inventory.ai;

import com.anshtechsolution.smart_inventory.ai.dto.DemandForecastResponse;
import com.anshtechsolution.smart_inventory.ai.dto.InventoryInsightResponse;
import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.product.ProductRepository;
import com.anshtechsolution.smart_inventory.sales.SaleItemRepository;
import com.anshtechsolution.smart_inventory.sales.SalesVelocityProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAiServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private SalesVelocityProjection salesVelocityProjection;

    private InventoryAiService inventoryAiService;

    @BeforeEach
    void setUp() {
        inventoryAiService = new InventoryAiService(
                chatClientBuilder,
                productRepository,
                saleItemRepository
        );
    }

    // ---------------------------------------------------------
    // Gemini mock helper
    // ---------------------------------------------------------

    private void mockGeminiResponse() {

        when(chatClientBuilder.build()).thenReturn(chatClient);

        when(chatClient.prompt()).thenReturn(requestSpec);

        when(requestSpec.user(any(String.class)))
                .thenReturn(requestSpec);

        when(requestSpec.call()).thenReturn(responseSpec);

        when(responseSpec.content())
                .thenReturn("Mock AI analysis");
    }

    // ---------------------------------------------------------
    // Inventory insights tests
    // ---------------------------------------------------------

    @Test
    void generateInventoryInsightsShouldCalculateCorrectMetrics() {

        mockGeminiResponse();

        Product laptop = createProduct(
                1L, "Laptop", 10, 3,
                "LAP-001", new BigDecimal("500.00")
        );

        Product mouse = createProduct(
                2L, "Mouse", 2, 5,
                "MOU-001", new BigDecimal("200.00")
        );

        when(productRepository.findAll())
                .thenReturn(List.of(laptop, mouse));

        when(saleItemRepository.findSalesVelocity(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(salesVelocityProjection));

        when(salesVelocityProjection.getProductId()).thenReturn(1L);
        when(salesVelocityProjection.getProductName()).thenReturn("Laptop");
        when(salesVelocityProjection.getUnitsSold()).thenReturn(5L);

        InventoryInsightResponse response =
                inventoryAiService.generateInventoryInsights();

        assertThat(response.getTotalProducts()).isEqualTo(2L);
        assertThat(response.getTotalUnitsInStock()).isEqualTo(12L);
        assertThat(response.getLowStockProducts()).isEqualTo(1L);

        // (10 * 500) + (2 * 200) = 5400
        assertThat(response.getTotalStockValueAtUnitPrice())
                .isEqualByComparingTo("5400.00");

        assertThat(response.getAnalysis())
                .isEqualTo("Mock AI analysis");

        assertThat(response.getGeneratedAt()).isNotNull();

        verify(chatClientBuilder).build();
    }

    @Test
    void generateInventoryInsightsShouldHandleEmptyInventory() {

        mockGeminiResponse();

        when(productRepository.findAll())
                .thenReturn(List.of());

        when(saleItemRepository.findSalesVelocity(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        InventoryInsightResponse response =
                inventoryAiService.generateInventoryInsights();

        assertThat(response.getTotalProducts()).isZero();
        assertThat(response.getTotalUnitsInStock()).isZero();
        assertThat(response.getLowStockProducts()).isZero();

        assertThat(response.getTotalStockValueAtUnitPrice())
                .isEqualByComparingTo("0.00");

        assertThat(response.getAnalysis())
                .isEqualTo("Mock AI analysis");
    }

    @Test
    void generateInventoryInsightsShouldHandleNullQuantityAndPrice() {

        mockGeminiResponse();

        Product product = new Product();
        product.setId(1L);
        product.setName("Keyboard");
        product.setQuantity(null);
        product.setReorderLevel(5);
        product.setUnitPrice(null);

        when(productRepository.findAll())
                .thenReturn(List.of(product));

        when(saleItemRepository.findSalesVelocity(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        InventoryInsightResponse response =
                inventoryAiService.generateInventoryInsights();

        assertThat(response.getTotalProducts()).isEqualTo(1L);
        assertThat(response.getTotalUnitsInStock()).isZero();
        assertThat(response.getLowStockProducts()).isEqualTo(1L);

        assertThat(response.getTotalStockValueAtUnitPrice())
                .isEqualByComparingTo("0.00");
    }

    // ---------------------------------------------------------
    // Demand forecast tests
    // ---------------------------------------------------------

    @Test
    void generateDemandForecastShouldCalculateCorrectForecast() {

        mockGeminiResponse();

        Product laptop = createProduct(
                1L, "Laptop", 20, 5,
                "LAP-001", new BigDecimal("500.00")
        );

        when(salesVelocityProjection.getProductId()).thenReturn(1L);
        when(salesVelocityProjection.getUnitsSold()).thenReturn(30L);

        when(saleItemRepository.findSalesVelocity(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(salesVelocityProjection));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(laptop));

        DemandForecastResponse response =
                inventoryAiService.generateDemandForecast(30, 15);

        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("Laptop");
        assertThat(response.getHistoricalDays()).isEqualTo(30);
        assertThat(response.getForecastDays()).isEqualTo(15);

        assertThat(response.getUnitsSold()).isEqualTo(30L);
        assertThat(response.getCurrentStock()).isEqualTo(20);

        assertThat(response.getAverageDailyDemand())
                .isEqualByComparingTo("1.0000");

        assertThat(response.getEstimatedDemand()).isEqualTo(15);
        assertThat(response.getEstimatedStockGap()).isZero();

        assertThat(response.getAnalysis())
                .isEqualTo("Mock AI analysis");

        assertThat(response.getGeneratedAt()).isNotNull();
    }

    @Test
    void generateDemandForecastShouldCalculateStockGap() {

        mockGeminiResponse();

        Product laptop = createProduct(
                1L, "Laptop", 5, 2,
                "LAP-001", new BigDecimal("500.00")
        );

        when(salesVelocityProjection.getProductId()).thenReturn(1L);
        when(salesVelocityProjection.getUnitsSold()).thenReturn(60L);

        when(saleItemRepository.findSalesVelocity(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(salesVelocityProjection));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(laptop));

        DemandForecastResponse response =
                inventoryAiService.generateDemandForecast(30, 30);

        // 60 / 30 * 30 = 60 units
        assertThat(response.getEstimatedDemand()).isEqualTo(60);

        // 60 estimated demand - 5 current stock = 55
        assertThat(response.getEstimatedStockGap()).isEqualTo(55);
    }

    @Test
    void generateDemandForecastShouldReturnNoSalesMessageWhenHistoryIsEmpty() {

        when(saleItemRepository.findSalesVelocity(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        DemandForecastResponse response =
                inventoryAiService.generateDemandForecast(30, 15);

        assertThat(response.getHistoricalDays()).isEqualTo(30);
        assertThat(response.getForecastDays()).isEqualTo(15);
        assertThat(response.getCurrentStock()).isZero();
        assertThat(response.getUnitsSold()).isZero();
        assertThat(response.getEstimatedDemand()).isZero();
        assertThat(response.getEstimatedStockGap()).isZero();

        assertThat(response.getAverageDailyDemand())
                .isEqualByComparingTo("0.0000");

        assertThat(response.getAnalysis())
                .contains("No completed sales");

        verifyNoInteractions(chatClientBuilder);
    }

    @Test
    void generateDemandForecastShouldRejectInvalidPeriods() {

        assertThatThrownBy(() ->
                inventoryAiService.generateDemandForecast(0, 15)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be greater than zero");

        assertThatThrownBy(() ->
                inventoryAiService.generateDemandForecast(30, 0)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be greater than zero");

        verifyNoInteractions(saleItemRepository);
        verifyNoInteractions(chatClientBuilder);
    }

    @Test
    void generateDemandForecastShouldThrowWhenProductDoesNotExist() {

        when(salesVelocityProjection.getProductId()).thenReturn(99L);

        when(saleItemRepository.findSalesVelocity(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(salesVelocityProjection));

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inventoryAiService.generateDemandForecast(30, 15)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "Product not found for sales record: 99"
                );

        verifyNoInteractions(chatClientBuilder);
    }

    // ---------------------------------------------------------
    // Helper method
    // ---------------------------------------------------------

    private Product createProduct(
            Long id,
            String name,
            Integer quantity,
            Integer reorderLevel,
            String sku,
            BigDecimal unitPrice
    ) {

        Product product = new Product();

        product.setId(id);
        product.setName(name);
        product.setQuantity(quantity);
        product.setReorderLevel(reorderLevel);
        product.setSku(sku);
        product.setUnitPrice(unitPrice);

        return product;
    }
}