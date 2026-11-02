package com.anshtechsolution.smart_inventory.ai;

import com.anshtechsolution.smart_inventory.ai.dto.DemandForecastResponse;
import com.anshtechsolution.smart_inventory.ai.dto.InventoryInsightResponse;
import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.product.ProductRepository;

import com.anshtechsolution.smart_inventory.sales.SaleItemRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InventoryAiService {

    private final ChatClient.Builder chatClientBuilder;
    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;

    /**
     * Generate inventory insights.
     * Java calculates the numeric metrics; Gemini explains them.
     */
    public InventoryInsightResponse generateInventoryInsights() {

        List<Product> products = productRepository.findAll();

        long totalProducts = products.size();

        long totalUnitsInStock = 0L;
        long lowStockProducts = 0L;

        BigDecimal totalStockValue = BigDecimal.ZERO;

        for (Product product : products) {

            int quantity = product.getQuantity() == null
                    ? 0
                    : product.getQuantity();

            totalUnitsInStock += quantity;

            Integer reorderLevel = product.getReorderLevel();

            if (reorderLevel != null && quantity <= reorderLevel) {
                lowStockProducts++;
            }

            BigDecimal unitPrice = product.getUnitPrice();

            if (unitPrice != null && quantity > 0) {
                totalStockValue = totalStockValue.add(
                        unitPrice.multiply(BigDecimal.valueOf(quantity))
                );
            }
        }

        totalStockValue = totalStockValue.setScale(
                2,
                RoundingMode.HALF_UP
        );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(30);

        var salesVelocity = saleItemRepository.findSalesVelocity(start, now);

        String inventoryData = products.stream()
                .map(product -> String.format(
                        "Product ID: %s, Name: %s, SKU: %s, Category: %s, " +
                                "Quantity: %s, Reorder Level: %s, Unit Price: %s",
                        product.getId(),
                        product.getName(),
                        product.getSku(),
                        product.getCategory() == null
                                ? "Not specified"
                                : product.getCategory(),
                        product.getQuantity(),
                        product.getReorderLevel(),
                        product.getUnitPrice()
                ))
                .reduce((first, second) -> first + "\n" + second)
                .orElse("No products found.");

        String salesData = salesVelocity.stream()
                .map(sale -> String.format(
                        "Product ID: %s, Product Name: %s, Units Sold: %s",
                        sale.getProductId(),
                        sale.getProductName(),
                        sale.getUnitsSold()
                ))
                .reduce((first, second) -> first + "\n" + second)
                .orElse("No completed sales found in the last 30 days.");

        String prompt = """
            You are an inventory management analyst.

            Analyze the following inventory data and completed sales
            from the last 30 days.

            INVENTORY:
            %s

            COMPLETED SALES:
            %s

            BACKEND-CALCULATED METRICS:
            Total products: %d
            Total units in stock: %d
            Low-stock products: %d
            Total stock value at stored unit prices: %s

            Instructions:
            1. Use the supplied metrics as authoritative.
            2. Do not change or recalculate the supplied totals.
            3. Identify low-stock risks and possible overstock.
            4. Explain demand trends using the supplied sales data.
            5. Recommend practical inventory actions.
            6. Do not invent products, sales, or figures.
            7. Mention limitations when sales history is insufficient.
            8. Return a clear, concise business analysis.
            """.formatted(
                inventoryData,
                salesData,
                totalProducts,
                totalUnitsInStock,
                lowStockProducts,
                totalStockValue.toPlainString()
        );

        String analysis = chatClientBuilder.build()
                .prompt()
                .user(prompt)
                .call()
                .content();

        return InventoryInsightResponse.builder()
                .totalProducts(totalProducts)
                .totalUnitsInStock(totalUnitsInStock)
                .lowStockProducts(lowStockProducts)
                .totalStockValueAtUnitPrice(totalStockValue)
                .analysis(analysis)
                .generatedAt(now)
                .build();
    }

    /**
     * Generate a demand forecast for the product with the highest
     * reported sales velocity.
     */
    public DemandForecastResponse generateDemandForecast(
            int historicalDays,
            int forecastDays
    ) {

        if (historicalDays <= 0 || forecastDays <= 0) {
            throw new IllegalArgumentException(
                    "Historical days and forecast days must be greater than zero."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(historicalDays);

        var salesVelocity = saleItemRepository.findSalesVelocity(start, now);

        if (salesVelocity.isEmpty()) {
            return DemandForecastResponse.builder()
                    .historicalDays(historicalDays)
                    .forecastDays(forecastDays)
                    .currentStock(0)
                    .unitsSold(0L)
                    .averageDailyDemand(BigDecimal.ZERO.setScale(4))
                    .estimatedDemand(0)
                    .estimatedStockGap(0)
                    .analysis("No completed sales were found in the selected " +
                            "historical period. A reliable demand forecast " +
                            "cannot be established without sales history.")
                    .generatedAt(now)
                    .build();
        }

        // Select the product with the highest recorded unit sales.
        var topSellingProduct = salesVelocity.stream()
                .max((first, second) -> Long.compare(
                        first.getUnitsSold() == null
                                ? 0L : first.getUnitsSold(),
                        second.getUnitsSold() == null
                                ? 0L : second.getUnitsSold()
                ))
                .orElseThrow();

        Long productId = topSellingProduct.getProductId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "Product not found for sales record: " + productId
                ));

        long unitsSold = topSellingProduct.getUnitsSold() == null
                ? 0L
                : topSellingProduct.getUnitsSold();

        int currentStock = product.getQuantity() == null
                ? 0
                : product.getQuantity();

        BigDecimal averageDailyDemand = BigDecimal.valueOf(unitsSold)
                .divide(
                        BigDecimal.valueOf(historicalDays),
                        4,
                        RoundingMode.HALF_UP
                );

        // Round forecast demand upward so fractional units are not ignored.
        int estimatedDemand = BigDecimal.valueOf(unitsSold)
                .multiply(BigDecimal.valueOf(forecastDays))
                .divide(
                        BigDecimal.valueOf(historicalDays),
                        0,
                        RoundingMode.CEILING
                )
                .intValueExact();

        int estimatedStockGap = Math.max(
                0,
                estimatedDemand - currentStock
        );

        String prompt = """
            You are an inventory demand forecasting analyst.

            Explain this backend-calculated forecast.

            Product ID: %s
            Product name: %s
            Historical period: %d days
            Forecast period: %d days
            Units sold in historical period: %d
            Current stock: %d
            Average daily demand: %s units
            Estimated forecast demand: %d units
            Estimated stock gap: %d units

            Rules:
            1. Treat all supplied numeric metrics as authoritative.
            2. Do not modify, recalculate, or contradict those metrics.
            3. Explain whether current stock appears sufficient.
            4. Discuss the limitations of forecasting from historical averages.
            5. Do not claim that historical sales guarantee future demand.
            6. Do not invent additional sales figures.
            7. Recommend practical replenishment actions when appropriate.
            """.formatted(
                product.getId(),
                product.getName(),
                historicalDays,
                forecastDays,
                unitsSold,
                currentStock,
                averageDailyDemand.toPlainString(),
                estimatedDemand,
                estimatedStockGap
        );

        String analysis = chatClientBuilder.build()
                .prompt()
                .user(prompt)
                .call()
                .content();

        return DemandForecastResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .historicalDays(historicalDays)
                .forecastDays(forecastDays)
                .currentStock(currentStock)
                .unitsSold(unitsSold)
                .averageDailyDemand(averageDailyDemand)
                .estimatedDemand(estimatedDemand)
                .estimatedStockGap(estimatedStockGap)
                .analysis(analysis)
                .generatedAt(now)
                .build();
    }

    /**
     * Convenience overload for a 30-day historical period
     * and a 30-day forecast.
     */
    public DemandForecastResponse forecastDemand(int forecastDays) {
        return generateDemandForecast(30, forecastDays);
    }

}