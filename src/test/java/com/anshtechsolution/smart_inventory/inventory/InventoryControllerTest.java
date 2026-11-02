package com.anshtechsolution.smart_inventory.inventory;

import com.anshtechsolution.smart_inventory.inventory.dto.StockAdjustmentRequest;
import com.anshtechsolution.smart_inventory.inventory.dto.StockMovementResponse;
import com.anshtechsolution.smart_inventory.product.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    private static final String BASE_URL = "/api/v1/inventory";

    // 1. Stock in successfully
    @Test
    void stockInShouldReturn200() throws Exception {
        when(inventoryService.stockIn(
                eq(1L), any(StockAdjustmentRequest.class), eq("stock-in-001")
        )).thenReturn(null);

        mockMvc.perform(post(BASE_URL + "/1/stock-in")
                        .header("Idempotency-Key", "stock-in-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 10,
                                  "reference": "PO-101",
                                  "notes": "Received stock"
                                }
                                """))
                .andExpect(status().isOk());

        verify(inventoryService).stockIn(
                eq(1L), any(StockAdjustmentRequest.class), eq("stock-in-001")
        );
    }

    // 2. Stock out successfully
    @Test
    void stockOutShouldReturn200() throws Exception {
        when(inventoryService.stockOut(
                eq(1L), any(StockAdjustmentRequest.class), eq("stock-out-001")
        )).thenReturn(null);

        mockMvc.perform(post(BASE_URL + "/1/stock-out")
                        .header("Idempotency-Key", "stock-out-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 5,
                                  "reference": "SALE-101",
                                  "notes": "Sold stock"
                                }
                                """))
                .andExpect(status().isOk());

        verify(inventoryService).stockOut(
                eq(1L), any(StockAdjustmentRequest.class), eq("stock-out-001")
        );
    }

    // 3. Stock in with zero quantity
    @Test
    void stockInShouldReturn400WhenQuantityIsZero() throws Exception {
        mockMvc.perform(post(BASE_URL + "/1/stock-in")
                        .header("Idempotency-Key", "stock-in-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    // 4. Stock out with negative quantity
    @Test
    void stockOutShouldReturn400WhenQuantityIsNegative() throws Exception {
        mockMvc.perform(post(BASE_URL + "/1/stock-out")
                        .header("Idempotency-Key", "stock-out-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": -5}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    // 5. Stock adjustment without quantity
    @Test
    void stockInShouldReturn400WhenQuantityIsMissing() throws Exception {
        mockMvc.perform(post(BASE_URL + "/1/stock-in")
                        .header("Idempotency-Key", "stock-in-003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference": "PO-101"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    // 6. Stock adjustment without idempotency key
    @Test
    void stockInShouldReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        mockMvc.perform(post(BASE_URL + "/1/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 10}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    // 7. Reference exceeds maximum length
    @Test
    void stockInShouldReturn400WhenReferenceIsTooLong() throws Exception {
        String reference = "R".repeat(101);

        mockMvc.perform(post(BASE_URL + "/1/stock-in")
                        .header("Idempotency-Key", "stock-in-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 10, "reference": "%s"}
                                """.formatted(reference)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    // 8. Notes exceed maximum length
    @Test
    void stockOutShouldReturn400WhenNotesAreTooLong() throws Exception {
        String notes = "N".repeat(501);

        mockMvc.perform(post(BASE_URL + "/1/stock-out")
                        .header("Idempotency-Key", "stock-out-003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 5, "notes": "%s"}
                                """.formatted(notes)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    // 9. Low stock products
    @Test
    void getLowStockProductsShouldReturn200() throws Exception {
        when(inventoryService.getLowStockProducts(any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(BASE_URL + "/low-stock"))
                .andExpect(status().isOk());

        verify(inventoryService).getLowStockProducts(any());
    }

    // 10. Product movement history
    @Test
    void getMovementHistoryShouldReturn200() throws Exception {
        when(inventoryService.getMovementHistory(eq(1L), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(BASE_URL + "/1/movements"))
                .andExpect(status().isOk());

        verify(inventoryService).getMovementHistory(eq(1L), any());
    }

    // 11. All movement history
    @Test
    void getAllMovementHistoryShouldReturn200() throws Exception {
        when(inventoryService.getAllMovementHistory(any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(BASE_URL + "/movements"))
                .andExpect(status().isOk());

        verify(inventoryService).getAllMovementHistory(any());
    }

    // 12. Movement history by type
    @Test
    void getMovementHistoryByTypeShouldReturn200() throws Exception {
        when(inventoryService.getMovementHistoryByType(eq("IN"), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(BASE_URL + "/movements/type/IN"))
                .andExpect(status().isOk());

        verify(inventoryService).getMovementHistoryByType(eq("IN"), any());
    }

    // 13. Product movement history by type
    @Test
    void getProductMovementHistoryByTypeShouldReturn200() throws Exception {
        when(inventoryService.getProductMovementHistoryByType(
                eq(1L), eq("OUT"), any()
        )).thenReturn(Page.empty());

        mockMvc.perform(get(BASE_URL + "/1/movements/type/OUT"))
                .andExpect(status().isOk());

        verify(inventoryService).getProductMovementHistoryByType(
                eq(1L), eq("OUT"), any()
        );
    }

    // 14. Inventory analytics
    @Test
    void getInventoryAnalyticsShouldReturn200() throws Exception {
        when(inventoryService.getInventoryAnalytics()).thenReturn(null);

        mockMvc.perform(get(BASE_URL + "/analytics"))
                .andExpect(status().isOk());

        verify(inventoryService).getInventoryAnalytics();
    }

    // 15. Analytics by date range
    @Test
    void getAnalyticsByDateRangeShouldReturn200() throws Exception {
        when(inventoryService.getInventoryAnalyticsByDateRange(any(), any()))
                .thenReturn(null);

        mockMvc.perform(get(BASE_URL + "/analytics/date-range")
                        .param("startDate", "2026-01-01T00:00:00")
                        .param("endDate", "2026-01-31T23:59:59"))
                .andExpect(status().isOk());

        verify(inventoryService).getInventoryAnalyticsByDateRange(any(), any());
    }

    // 16. Analytics by product
    @Test
    void getProductInventoryAnalyticsShouldReturn200() throws Exception {
        when(inventoryService.getProductInventoryAnalytics(1L))
                .thenReturn(null);

        mockMvc.perform(get(BASE_URL + "/analytics/product/1"))
                .andExpect(status().isOk());

        verify(inventoryService).getProductInventoryAnalytics(1L);
    }

    // 17. Analytics by category
    @Test
    void getCategoryInventoryAnalyticsShouldReturn200() throws Exception {
        when(inventoryService.getCategoryInventoryAnalytics("Electronics"))
                .thenReturn(null);

        mockMvc.perform(get(BASE_URL + "/analytics/category/Electronics"))
                .andExpect(status().isOk());

        verify(inventoryService).getCategoryInventoryAnalytics("Electronics");
    }

    // 18. Analytics by supplier
    @Test
    void getSupplierInventoryAnalyticsShouldReturn200() throws Exception {
        when(inventoryService.getSupplierInventoryAnalytics(1L))
                .thenReturn(null);

        mockMvc.perform(get(BASE_URL + "/analytics/supplier/1"))
                .andExpect(status().isOk());

        verify(inventoryService).getSupplierInventoryAnalytics(1L);
    }

    // 19. Inventory dashboard
    @Test
    void getInventoryDashboardShouldReturn200() throws Exception {
        when(inventoryService.getInventoryDashboard()).thenReturn(null);

        mockMvc.perform(get(BASE_URL + "/dashboard"))
                .andExpect(status().isOk());

        verify(inventoryService).getInventoryDashboard();
    }

    // 20. Date range with invalid date format
    @Test
    void getAnalyticsByDateRangeShouldReturn400ForInvalidDate() throws Exception {
        mockMvc.perform(get(BASE_URL + "/analytics/date-range")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2026-01-31T23:59:59"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }
}