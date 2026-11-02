package com.anshtechsolution.smart_inventory.dashboard;

import com.anshtechsolution.smart_inventory.dashboard.dto.InventoryDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class InventoryDashboardController {

    private final InventoryDashboardService inventoryDashboardService;

    @GetMapping
    public InventoryDashboardResponse getDashboard() {
        return inventoryDashboardService.getDashboard();
    }
}