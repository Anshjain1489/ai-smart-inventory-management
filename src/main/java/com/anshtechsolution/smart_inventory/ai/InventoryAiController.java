package com.anshtechsolution.smart_inventory.ai;

import com.anshtechsolution.smart_inventory.ai.dto.DemandForecastResponse;
import com.anshtechsolution.smart_inventory.ai.dto.InventoryInsightResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class InventoryAiController {

    private final InventoryAiService inventoryAiService;
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/inventory-insights")
    public InventoryInsightResponse getInventoryInsights() {
        return inventoryAiService.generateInventoryInsights();
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/demand-forecast")
    public DemandForecastResponse getDemandForecast(
            @RequestParam(defaultValue = "30") int days) {

        if (days < 7 || days > 90) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Forecast days must be between 7 and 90");
        }

        return inventoryAiService.forecastDemand(days);
    }
}