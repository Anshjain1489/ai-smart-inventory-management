package com.anshtechsolution.smart_inventory.sales;

import com.anshtechsolution.smart_inventory.sales.dto.CreateSaleRequest;
import com.anshtechsolution.smart_inventory.sales.dto.SaleResponse;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SalesController.class)
@AutoConfigureMockMvc(addFilters = false)
class SalesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalesService salesService;

    private SaleResponse createSaleResponse() {
        return SaleResponse.builder()
                .id(1L)
                .saleNumber("SALE-2026-001")
                .customerName("Rahul")
                .totalAmount(new BigDecimal("100000.00"))
                .status("COMPLETED")
                .build();
    }

    // 1. Create sale successfully
    @Test
    void createSaleShouldReturn201() throws Exception {

        when(salesService.createSale(any(CreateSaleRequest.class)))
                .thenReturn(createSaleResponse());

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Rahul",
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.saleNumber")
                        .value("SALE-2026-001"))
                .andExpect(jsonPath("$.customerName").value("Rahul"))
                .andExpect(jsonPath("$.totalAmount").value(100000.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(salesService).createSale(any(CreateSaleRequest.class));
    }

    // 2. Reject sale with empty items
    @Test
    void createSaleShouldReturn400WhenItemsAreEmpty()
            throws Exception {

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Rahul",
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(salesService);
    }

    // 3. Reject sale when items are missing
    @Test
    void createSaleShouldReturn400WhenItemsAreMissing()
            throws Exception {

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Rahul"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(salesService);
    }

    // 4. Reject invalid product ID
    @Test
    void createSaleShouldReturn400WhenProductIdIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Rahul",
                                  "items": [
                                    {
                                      "productId": 0,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(salesService);
    }

    // 5. Reject invalid quantity
    @Test
    void createSaleShouldReturn400WhenQuantityIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Rahul",
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(salesService);
    }

    // 6. Reject customer name exceeding 150 characters
    @Test
    void createSaleShouldReturn400WhenCustomerNameIsTooLong()
            throws Exception {

        String longName = "A".repeat(151);

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "%s",
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(salesService);
    }

    // 7. Get sale by ID
    @Test
    void getSaleShouldReturn200() throws Exception {

        when(salesService.getSale(1L))
                .thenReturn(createSaleResponse());

        mockMvc.perform(get("/api/v1/sales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.saleNumber")
                        .value("SALE-2026-001"))
                .andExpect(jsonPath("$.customerName").value("Rahul"));

        verify(salesService).getSale(1L);
    }
}