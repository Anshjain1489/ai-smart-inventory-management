package com.anshtechsolution.smart_inventory.inventory;

import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.inventory.dto.InventoryRiskResponse;
import com.anshtechsolution.smart_inventory.product.ProductRepository;
import com.anshtechsolution.smart_inventory.sales.SaleItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import com.anshtechsolution.smart_inventory.inventory.dto.InventoryAlertResponse;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class InventoryRiskService {

    private static final int HISTORICAL_DAYS = 30;
    private static final int MEDIUM_COVERAGE_DAYS = 90;
    private static final int HIGH_COVERAGE_DAYS = 180;

    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;

    public List<InventoryRiskResponse> evaluateAllProducts() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(HISTORICAL_DAYS);

        var salesVelocity =
                saleItemRepository.findSalesVelocity(start, now);

        return productRepository.findAll()
                .stream()
                .map(product -> {
                    long unitsSold = salesVelocity.stream()
                            .filter(sale -> sale.getProductId()
                                    .equals(product.getId()))
                            .mapToLong(sale -> sale.getUnitsSold() == null
                                    ? 0L
                                    : sale.getUnitsSold())
                            .sum();

                    return evaluateProduct(
                            product,
                            unitsSold,
                            HISTORICAL_DAYS,
                            now
                    );
                })
                .toList();
    }

    public InventoryRiskResponse evaluateProduct(
            Product product,
            long unitsSold,
            int historicalDays,
            LocalDateTime evaluatedAt
    ) {
        if (historicalDays <= 0) {
            throw new IllegalArgumentException(
                    "Historical days must be greater than zero."
            );
        }

        int stock = product.getQuantity() == null
                ? 0
                : product.getQuantity();

        Integer reorderLevel = product.getReorderLevel();

        BigDecimal averageDailyDemand = BigDecimal.valueOf(unitsSold)
                .divide(
                        BigDecimal.valueOf(historicalDays),
                        4,
                        RoundingMode.HALF_UP
                );

        BigDecimal coverageDays = null;

        if (averageDailyDemand.compareTo(BigDecimal.ZERO) > 0) {
            coverageDays = BigDecimal.valueOf(stock)
                    .divide(
                            averageDailyDemand,
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        InventoryRiskLevel riskLevel;
        String riskReason;

        if (stock <= 0) {
            riskLevel = InventoryRiskLevel.CRITICAL;
            riskReason = "Product is out of stock or has a non-positive quantity.";

        } else if (reorderLevel != null && stock <= reorderLevel) {
            riskLevel = InventoryRiskLevel.HIGH;
            riskReason = "Current stock is at or below the reorder level.";

        } else if (unitsSold < 0) {
            riskLevel = InventoryRiskLevel.UNKNOWN;
            riskReason = "Sales data is invalid; demand cannot be assessed.";

        } else if (unitsSold == 0) {
            riskLevel = InventoryRiskLevel.UNKNOWN;
            riskReason = "No units sold during the historical period; " +
                    "stock coverage cannot be reliably estimated.";

        } else if (coverageDays.compareTo(
                BigDecimal.valueOf(HIGH_COVERAGE_DAYS)) > 0) {
            riskLevel = InventoryRiskLevel.HIGH;
            riskReason = "Estimated stock coverage exceeds 180 days.";

        } else if (coverageDays.compareTo(
                BigDecimal.valueOf(MEDIUM_COVERAGE_DAYS)) > 0) {
            riskLevel = InventoryRiskLevel.MEDIUM;
            riskReason = "Estimated stock coverage exceeds 90 days.";

        } else {
            riskLevel = InventoryRiskLevel.NORMAL;
            riskReason = "No configured stock risk threshold has been exceeded.";
        }

        return InventoryRiskResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .currentStock(stock)
                .reorderLevel(reorderLevel)
                .unitsSold(unitsSold)
                .historicalDays(historicalDays)
                .averageDailyDemand(averageDailyDemand)
                .estimatedCoverageDays(coverageDays)
                .riskLevel(riskLevel)
                .riskReason(riskReason)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public List<InventoryAlertResponse> generateAlerts() {

        LocalDateTime now = LocalDateTime.now();

        List<InventoryAlertResponse> alerts = new ArrayList<>();

        List<InventoryRiskResponse> risks = evaluateAllProducts();

        for (InventoryRiskResponse risk : risks) {

            InventoryAlertType alertType = null;
            String message = null;

            if (risk.getCurrentStock() == null || risk.getCurrentStock() <= 0) {

                alertType = InventoryAlertType.OUT_OF_STOCK;
                message = "Product is out of stock. Review replenishment needs.";

            } else if (risk.getReorderLevel() != null
                    && risk.getCurrentStock() <= risk.getReorderLevel()) {

                alertType = InventoryAlertType.LOW_STOCK;
                message = "Stock is at or below the reorder level.";

            } else if (risk.getEstimatedCoverageDays() != null
                    && risk.getEstimatedCoverageDays()
                    .compareTo(java.math.BigDecimal.valueOf(180)) > 0) {

                alertType = InventoryAlertType.OVERSTOCK;
                message = "Estimated stock coverage exceeds 180 days. Review demand and purchasing.";

            } else if (risk.getUnitsSold() == null || risk.getUnitsSold() == 0) {

                alertType = InventoryAlertType.NO_SALES_DATA;
                message = "No units sold in the historical period. Coverage cannot be reliably estimated.";
            }

            if (alertType != null) {
                alerts.add(InventoryAlertResponse.builder()
                        .productId(risk.getProductId())
                        .productName(risk.getProductName())
                        .sku(risk.getSku())
                        .alertType(alertType)
                        .riskLevel(risk.getRiskLevel())
                        .currentStock(risk.getCurrentStock())
                        .reorderLevel(risk.getReorderLevel())
                        .message(message)
                        .createdAt(now)
                        .build());
            }
        }

        return alerts;
    }
}