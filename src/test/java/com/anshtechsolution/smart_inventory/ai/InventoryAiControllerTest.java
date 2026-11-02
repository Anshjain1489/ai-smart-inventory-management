package com.anshtechsolution.smart_inventory.ai;

import com.anshtechsolution.smart_inventory.ai.dto.DemandForecastResponse;
import com.anshtechsolution.smart_inventory.ai.dto.InventoryInsightResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryAiController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryAiService inventoryAiService;

    private static final String BASE_URL = "/api/v1/ai";

    // 1. Inventory insights successfully
    @Test
    void getInventoryInsightsShouldReturn200() throws Exception {

        InventoryInsightResponse response = InventoryInsightResponse.builder()
                .totalProducts(100L)
                .totalUnitsInStock(500L)
                .lowStockProducts(10L)
                .totalStockValueAtUnitPrice(new BigDecimal("250000.00"))
                .analysis("Inventory is healthy overall.")
                .generatedAt(LocalDateTime.now())
                .build();

        when(inventoryAiService.generateInventoryInsights())
                .thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/inventory-insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(100))
                .andExpect(jsonPath("$.totalUnitsInStock").value(500))
                .andExpect(jsonPath("$.lowStockProducts").value(10))
                .andExpect(jsonPath("$.totalStockValueAtUnitPrice")
                        .value(250000.00))
                .andExpect(jsonPath("$.analysis")
                        .value("Inventory is healthy overall."))
                .andExpect(jsonPath("$.generatedAt").exists());

        verify(inventoryAiService).generateInventoryInsights();
    }

    // 2. Demand forecast with default 30 days
    @Test
    void getDemandForecastShouldUseDefault30Days() throws Exception {

        DemandForecastResponse response = DemandForecastResponse.builder()
                .productId(1L)
                .productName("Laptop")
                .historicalDays(30)
                .forecastDays(30)
                .currentStock(20)
                .unitsSold(60L)
                .averageDailyDemand(new BigDecimal("2.00"))
                .estimatedDemand(60)
                .estimatedStockGap(40)
                .analysis("Additional stock may be required.")
                .generatedAt(LocalDateTime.now())
                .build();

        when(inventoryAiService.forecastDemand(30))
                .thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/demand-forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.historicalDays").value(30))
                .andExpect(jsonPath("$.forecastDays").value(30))
                .andExpect(jsonPath("$.currentStock").value(20))
                .andExpect(jsonPath("$.unitsSold").value(60))
                .andExpect(jsonPath("$.estimatedDemand").value(60))
                .andExpect(jsonPath("$.estimatedStockGap").value(40))
                .andExpect(jsonPath("$.analysis")
                        .value("Additional stock may be required."));

        verify(inventoryAiService).forecastDemand(30);
    }

    // 3. Demand forecast with custom days
    @Test
    void getDemandForecastShouldAcceptCustomDays() throws Exception {

        DemandForecastResponse response = DemandForecastResponse.builder()
                .productId(2L)
                .productName("Keyboard")
                .forecastDays(60)
                .build();

        when(inventoryAiService.forecastDemand(60))
                .thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/demand-forecast")
                        .param("days", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.forecastDays").value(60));

        verify(inventoryAiService).forecastDemand(60);
    }

    // 4. Minimum allowed forecast period
    @Test
    void getDemandForecastShouldAcceptMinimum7Days() throws Exception {

        when(inventoryAiService.forecastDemand(7))
                .thenReturn(DemandForecastResponse.builder()
                        .forecastDays(7)
                        .build());

        mockMvc.perform(get(BASE_URL + "/demand-forecast")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecastDays").value(7));

        verify(inventoryAiService).forecastDemand(7);
    }

    // 5. Maximum allowed forecast period
    @Test
    void getDemandForecastShouldAcceptMaximum90Days() throws Exception {

        when(inventoryAiService.forecastDemand(90))
                .thenReturn(DemandForecastResponse.builder()
                        .forecastDays(90)
                        .build());

        mockMvc.perform(get(BASE_URL + "/demand-forecast")
                        .param("days", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecastDays").value(90));

        verify(inventoryAiService).forecastDemand(90);
    }

    // 6. Reject forecast below 7 days
    @Test
    void getDemandForecastShouldRejectDaysBelow7() throws Exception {

        mockMvc.perform(get(BASE_URL + "/demand-forecast")
                        .param("days", "6"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryAiService);
    }

    // 7. Reject forecast above 90 days
    @Test
    void getDemandForecastShouldRejectDaysAbove90() throws Exception {

        mockMvc.perform(get(BASE_URL + "/demand-forecast")
                        .param("days", "91"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryAiService);
    }

    // 8. Reject zero days
    @Test
    void getDemandForecastShouldRejectZeroDays() throws Exception {

        mockMvc.perform(get(BASE_URL + "/demand-forecast")
                        .param("days", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryAiService);
    }

    // 9. Reject negative days
    @Test
    void getDemandForecastShouldRejectNegativeDays() throws Exception {

        mockMvc.perform(get(BASE_URL + "/demand-forecast")
                        .param("days", "-10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryAiService);
    }

    // 10. Reject non-numeric days
    @Test
    void getDemandForecastShouldRejectNonNumericDays() throws Exception {

        mockMvc.perform(get(BASE_URL + "/demand-forecast")
                        .param("days", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryAiService);
    }

    // 11. Reject missing days parameter? No: default should apply.
    @Test
    void getDemandForecastShouldReturnDefaultWhenDaysParameterIsMissing()
            throws Exception {

        when(inventoryAiService.forecastDemand(30))
                .thenReturn(DemandForecastResponse.builder()
                        .forecastDays(30)
                        .build());

        mockMvc.perform(get(BASE_URL + "/demand-forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecastDays").value(30));

        verify(inventoryAiService).forecastDemand(30);
    }
}