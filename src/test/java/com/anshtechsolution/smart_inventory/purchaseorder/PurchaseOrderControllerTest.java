package com.anshtechsolution.smart_inventory.purchaseorder;

import com.anshtechsolution.smart_inventory.purchaseorder.dto.*;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseOrderService purchaseOrderService;

    private PurchaseOrderResponseDTO createPurchaseOrderResponse() {

        PurchaseOrderItemResponseDTO item =
                new PurchaseOrderItemResponseDTO(
                        1L,
                        10L,
                        "Dell Laptop",
                        10,
                        0,
                        10,
                        new BigDecimal("50000.00"),
                        new BigDecimal("500000.00")
                );

        return new PurchaseOrderResponseDTO(
                1L,
                2L,
                "ABC Suppliers",
                PurchaseOrderStatus.DRAFT,
                null,
                null,
                new BigDecimal("500000.00"),
                "Monthly stock purchase",
                null,
                null,
                List.of(item)
        );
    }

    // 1. Create purchase order
    @Test
    void createPurchaseOrderShouldReturn201() throws Exception {

        when(purchaseOrderService.createPurchaseOrder(
                any(PurchaseOrderRequestDTO.class)
        )).thenReturn(createPurchaseOrderResponse());

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": 2,
                                  "notes": "Monthly stock purchase",
                                  "items": [
                                    {
                                      "productId": 10,
                                      "orderedQuantity": 10,
                                      "unitPrice": 50000.00
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.supplierId").value(2))
                .andExpect(jsonPath("$.supplierName")
                        .value("ABC Suppliers"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].productId").value(10));

        verify(purchaseOrderService).createPurchaseOrder(
                any(PurchaseOrderRequestDTO.class)
        );
    }

    // 2. Reject missing supplier ID
    @Test
    void createPurchaseOrderShouldReturn400WhenSupplierIdIsMissing()
            throws Exception {

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": 10,
                                      "orderedQuantity": 10,
                                      "unitPrice": 50000.00
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }

    // 3. Reject empty purchase order items
    @Test
    void createPurchaseOrderShouldReturn400WhenItemsAreEmpty()
            throws Exception {

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": 2,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }

    // 4. Reject invalid nested product ID
    @Test
    void createPurchaseOrderShouldReturn400WhenProductIdIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": 2,
                                  "items": [
                                    {
                                      "productId": 0,
                                      "orderedQuantity": 10,
                                      "unitPrice": 50000.00
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }

    // 5. Reject invalid ordered quantity
    @Test
    void createPurchaseOrderShouldReturn400WhenQuantityIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": 2,
                                  "items": [
                                    {
                                      "productId": 10,
                                      "orderedQuantity": 0,
                                      "unitPrice": 50000.00
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }

    // 6. Reject negative unit price
    @Test
    void createPurchaseOrderShouldReturn400WhenUnitPriceIsNegative()
            throws Exception {

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": 2,
                                  "items": [
                                    {
                                      "productId": 10,
                                      "orderedQuantity": 10,
                                      "unitPrice": -100.00
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }

    // 7. Get all purchase orders
    @Test
    void getAllPurchaseOrdersShouldReturn200() throws Exception {

        when(purchaseOrderService.getAllPurchaseOrders())
                .thenReturn(List.of(createPurchaseOrderResponse()));

        mockMvc.perform(get("/api/v1/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].supplierName")
                        .value("ABC Suppliers"));

        verify(purchaseOrderService).getAllPurchaseOrders();
    }

    // 8. Get purchase order by ID
    @Test
    void getPurchaseOrderByIdShouldReturn200() throws Exception {

        when(purchaseOrderService.getPurchaseOrderById(1L))
                .thenReturn(createPurchaseOrderResponse());

        mockMvc.perform(get("/api/v1/purchase-orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(purchaseOrderService).getPurchaseOrderById(1L);
    }

    // 9. Get purchase orders by supplier
    @Test
    void getPurchaseOrdersBySupplierShouldReturn200()
            throws Exception {

        when(purchaseOrderService.getPurchaseOrdersBySupplier(2L))
                .thenReturn(List.of(createPurchaseOrderResponse()));

        mockMvc.perform(get("/api/v1/purchase-orders/supplier/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierId").value(2));

        verify(purchaseOrderService).getPurchaseOrdersBySupplier(2L);
    }

    // 10. Get purchase orders by status
    @Test
    void getPurchaseOrdersByStatusShouldReturn200()
            throws Exception {

        when(purchaseOrderService.getPurchaseOrdersByStatus(
                PurchaseOrderStatus.DRAFT
        )).thenReturn(List.of(createPurchaseOrderResponse()));

        mockMvc.perform(get("/api/v1/purchase-orders/status/DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DRAFT"));

        verify(purchaseOrderService).getPurchaseOrdersByStatus(
                PurchaseOrderStatus.DRAFT
        );
    }

    // 11. Reject invalid purchase order status
    @Test
    void getPurchaseOrdersByStatusShouldReturn400WhenStatusIsInvalid()
            throws Exception {

        mockMvc.perform(get("/api/v1/purchase-orders/status/INVALID"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }

    // 12. Place purchase order
    @Test
    void placePurchaseOrderShouldReturn200() throws Exception {

        when(purchaseOrderService.placePurchaseOrder(1L))
                .thenReturn(createPurchaseOrderResponse());

        mockMvc.perform(patch("/api/v1/purchase-orders/1/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(purchaseOrderService).placePurchaseOrder(1L);
    }

    // 13. Receive stock
    @Test
    void receiveStockShouldReturn200() throws Exception {

        when(purchaseOrderService.receiveStock(
                eq(1L),
                eq(1L),
                any(ReceiveStockRequestDTO.class)
        )).thenReturn(createPurchaseOrderResponse());

        mockMvc.perform(post(
                        "/api/v1/purchase-orders/1/items/1/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receivedQuantity": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(purchaseOrderService).receiveStock(
                eq(1L),
                eq(1L),
                any(ReceiveStockRequestDTO.class)
        );
    }

    // 14. Reject zero received quantity
    @Test
    void receiveStockShouldReturn400WhenQuantityIsZero()
            throws Exception {

        mockMvc.perform(post(
                        "/api/v1/purchase-orders/1/items/1/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receivedQuantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }

    // 15. Reject missing received quantity
    @Test
    void receiveStockShouldReturn400WhenQuantityIsMissing()
            throws Exception {

        mockMvc.perform(post(
                        "/api/v1/purchase-orders/1/items/1/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }
}