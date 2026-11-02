package com.anshtechsolution.smart_inventory.analytics;

import com.anshtechsolution.smart_inventory.analytics.dto.DailySalesResponse;
import com.anshtechsolution.smart_inventory.analytics.dto.SalesSummaryResponse;
import com.anshtechsolution.smart_inventory.analytics.dto.TopProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sales-summary")
    public SalesSummaryResponse getSummary(
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate to) {

        return analyticsService.getSummary(from, to);
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> getTopProducts(
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate to) {

        return analyticsService.getTopProducts(from, to);
    }

    @GetMapping("/daily-sales")
    public List<DailySalesResponse> getDailySales(
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate to) {

        return analyticsService.getDailySales(from, to);
    }
}