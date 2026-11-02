package com.anshtechsolution.smart_inventory.dashboard;

import com.anshtechsolution.smart_inventory.dashboard.dto.InventoryDashboardResponse;
import com.anshtechsolution.smart_inventory.inventory.InventoryAlertType;
import com.anshtechsolution.smart_inventory.inventory.InventoryRiskService;
import com.anshtechsolution.smart_inventory.inventory.dto.InventoryAlertResponse;
import com.anshtechsolution.smart_inventory.inventory.dto.InventoryRiskResponse;
import com.anshtechsolution.smart_inventory.inventory.InventoryRiskLevel;
import com.anshtechsolution.smart_inventory.product.Product;
import com.anshtechsolution.smart_inventory.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryDashboardService {

    private final ProductRepository productRepository;
    private final InventoryRiskService inventoryRiskService;

    public InventoryDashboardResponse getDashboard() {

        List<Product> products = productRepository.findAll();

        long totalProducts = products.size();

        long totalUnitsInStock = products.stream()
                .mapToLong(product ->
                        product.getQuantity() == null ? 0 : product.getQuantity())
                .sum();

        long lowStockProducts = products.stream()
                .filter(product -> product.getQuantity() != null
                        && product.getQuantity() > 0
                        && product.getReorderLevel() != null
                        && product.getQuantity() <= product.getReorderLevel())
                .count();

        long outOfStockProducts = products.stream()
                .filter(product -> product.getQuantity() == null
                        || product.getQuantity() <= 0)
                .count();

        BigDecimal totalStockValue = products.stream()
                .filter(product -> product.getQuantity() != null
                        && product.getUnitPrice() != null)
                .map(product -> product.getUnitPrice()
                        .multiply(BigDecimal.valueOf(product.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InventoryAlertResponse> alerts =
                inventoryRiskService.generateAlerts();

        long overstockAlerts = alerts.stream()
                .filter(alert ->
                        alert.getAlertType() == InventoryAlertType.OVERSTOCK)
                .count();

        long productsWithNoSalesData = alerts.stream()
                .filter(alert ->
                        alert.getAlertType() == InventoryAlertType.NO_SALES_DATA)
                .count();

        return InventoryDashboardResponse.builder()
                .totalProducts(totalProducts)
                .totalUnitsInStock(totalUnitsInStock)
                .lowStockProducts(lowStockProducts)
                .outOfStockProducts(outOfStockProducts)
                .totalStockValueAtUnitPrice(totalStockValue)
                .overstockAlerts(overstockAlerts)
                .productsWithNoSalesData(productsWithNoSalesData)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}