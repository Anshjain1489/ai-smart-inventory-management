package com.anshtechsolution.smart_inventory.report;

import com.anshtechsolution.smart_inventory.product.ProductRepository;
import com.anshtechsolution.smart_inventory.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryReportService {

    private final ProductRepository productRepository;

    public InventorySummaryReportDTO getInventorySummary() {
        return productRepository.getInventorySummary();
    }

    public StockStatusReportDTO getStockStatusReport() {
        return productRepository.getStockStatusReport();
    }

    public List<CategoryInventoryReportDTO> getCategoryInventoryReport() {
        return productRepository.getCategoryInventoryReport();
    }

    public InventoryValuationReportDTO getInventoryValuation() {

        List<InventoryValuationItemDTO> products =
                productRepository.getInventoryValuationItems();

        long totalProducts = products.size();

        long totalUnits = products.stream()
                .mapToLong(product -> product.quantity())
                .sum();

        BigDecimal totalInventoryValue = products.stream()
                .map(InventoryValuationItemDTO::inventoryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventoryValuationReportDTO(
                totalProducts,
                totalUnits,
                totalInventoryValue,
                products
        );
    }

    public DateFilteredInventoryReportDTO getInventoryReportByDate(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "End date must be on or after start date"
            );
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        return productRepository.getInventoryReportByDate(
                startDateTime,
                endDateTime
        );
    }
}