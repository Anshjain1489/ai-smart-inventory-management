package com.anshtechsolution.smart_inventory.inventory;

import com.anshtechsolution.smart_inventory.inventory.InventoryRiskService;
import com.anshtechsolution.smart_inventory.inventory.dto.InventoryAlertResponse;
import com.anshtechsolution.smart_inventory.inventory.dto.InventoryRiskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory-risk")
@RequiredArgsConstructor
public class InventoryRiskController {

    private final InventoryRiskService inventoryRiskService;

    @GetMapping
    public List<InventoryRiskResponse> getAllInventoryRisks() {
        return inventoryRiskService.evaluateAllProducts();
    }

    @GetMapping("/alerts")
    public List<InventoryAlertResponse> getInventoryAlerts() {
        return inventoryRiskService.generateAlerts();
    }
}