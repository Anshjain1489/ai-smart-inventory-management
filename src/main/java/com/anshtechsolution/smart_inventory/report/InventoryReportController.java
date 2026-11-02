package com.anshtechsolution.smart_inventory.report;

import com.anshtechsolution.smart_inventory.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class InventoryReportController {

    private final InventoryReportService inventoryReportService;

    @GetMapping("/inventory-summary")
    public InventorySummaryReportDTO getInventorySummary() {
        return inventoryReportService.getInventorySummary();
    }

    @GetMapping("/stock-status")
    public StockStatusReportDTO getStockStatusReport() {
        return inventoryReportService.getStockStatusReport();
    }

    @GetMapping("/category-summary")
    public List<CategoryInventoryReportDTO> getCategoryInventoryReport() {
        return inventoryReportService.getCategoryInventoryReport();
    }

    @GetMapping("/inventory-valuation")
    public InventoryValuationReportDTO getInventoryValuation() {
        return inventoryReportService.getInventoryValuation();
    }

    @GetMapping("/inventory-by-date")
    public DateFilteredInventoryReportDTO getInventoryReportByDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return inventoryReportService.getInventoryReportByDate(
                startDate,
                endDate
        );
    }
}