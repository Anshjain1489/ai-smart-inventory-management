package com.anshtechsolution.smart_inventory.product;

import com.anshtechsolution.smart_inventory.product.dto.ProductCreateRequest;
import com.anshtechsolution.smart_inventory.product.dto.ProductResponse;
import com.anshtechsolution.smart_inventory.product.dto.ProductUpdateRequest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ProductResponse createProductResponse() {
        return ProductResponse.builder()
                .id(1L)
                .sku("LAPTOP-001")
                .name("Dell Laptop")
                .description("Business laptop")
                .category("Electronics")
                .supplierId(1L)
                .quantity(20)
                .reorderLevel(5)
                .unitPrice(new BigDecimal("50000.00"))
                .build();
    }

    private Page<ProductResponse> createProductPage() {
        return new PageImpl<>(
                List.of(createProductResponse()),
                PageRequest.of(0, 10),
                1
        );
    }

    // 1. Create product successfully
    @Test
    void createProductShouldReturn201() throws Exception {

        when(productService.create(any(ProductCreateRequest.class)))
                .thenReturn(createProductResponse());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "LAPTOP-001",
                                  "name": "Dell Laptop",
                                  "description": "Business laptop",
                                  "category": "Electronics",
                                  "supplierId": 1,
                                  "quantity": 20,
                                  "reorderLevel": 5,
                                  "unitPrice": 50000.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.name").value("Dell Laptop"))
                .andExpect(jsonPath("$.quantity").value(20));

        verify(productService).create(any(ProductCreateRequest.class));
    }

    // 2. Reject invalid product request
    @Test
    void createProductShouldReturn400WhenRequestIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "",
                                  "name": "",
                                  "quantity": -5,
                                  "reorderLevel": -1,
                                  "unitPrice": -100
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    // 3. Get product by ID
    @Test
    void getProductByIdShouldReturn200() throws Exception {

        when(productService.getById(1L))
                .thenReturn(createProductResponse());

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Dell Laptop"));

        verify(productService).getById(1L);
    }

    // 4. Get all products
    @Test
    void getAllProductsShouldReturn200() throws Exception {

        when(productService.getAll(any(Pageable.class)))
                .thenReturn(createProductPage());

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name")
                        .value("Dell Laptop"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService).getAll(any(Pageable.class));
    }

    // 5. Get products by category
    @Test
    void getProductsByCategoryShouldReturn200() throws Exception {

        when(productService.getByCategory(
                eq("Electronics"),
                any(Pageable.class)
        )).thenReturn(createProductPage());

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category")
                        .value("Electronics"));

        verify(productService).getByCategory(
                eq("Electronics"),
                any(Pageable.class)
        );
    }

    // 6. Update product
    @Test
    void updateProductShouldReturn200() throws Exception {

        when(productService.update(
                eq(1L),
                any(ProductUpdateRequest.class)
        )).thenReturn(createProductResponse());

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "LAPTOP-001",
                                  "name": "Dell Laptop",
                                  "description": "Updated laptop",
                                  "category": "Electronics",
                                  "supplierId": 1,
                                  "quantity": 25,
                                  "reorderLevel": 5,
                                  "unitPrice": 52000.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(productService).update(
                eq(1L),
                any(ProductUpdateRequest.class)
        );
    }

    // 7. Reject invalid update request
    @Test
    void updateProductShouldReturn400WhenRequestIsInvalid()
            throws Exception {

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "",
                                  "name": "",
                                  "quantity": -10,
                                  "reorderLevel": -5,
                                  "unitPrice": -100
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    // 8. Delete product
    @Test
    void deleteProductShouldReturn204() throws Exception {

        doNothing().when(productService).delete(1L);

        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).delete(1L);
    }

    // 9. Search products
    @Test
    void searchProductsShouldReturn200() throws Exception {

        when(productService.searchByKeyword(
                eq("Dell"),
                any(Pageable.class)
        )).thenReturn(createProductPage());

        mockMvc.perform(get("/api/v1/products/search")
                        .param("keyword", "Dell"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name")
                        .value("Dell Laptop"));

        verify(productService).searchByKeyword(
                eq("Dell"),
                any(Pageable.class)
        );
    }

    // 10. Search products by keyword and category
    @Test
    void searchByKeywordAndCategoryShouldReturn200()
            throws Exception {

        when(productService.searchByKeywordAndCategory(
                eq("Dell"),
                eq("Electronics"),
                any(Pageable.class)
        )).thenReturn(createProductPage());

        mockMvc.perform(get("/api/v1/products/search/filter")
                        .param("keyword", "Dell")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService).searchByKeywordAndCategory(
                eq("Dell"),
                eq("Electronics"),
                any(Pageable.class)
        );
    }

    // 11. Filter by stock status
    @Test
    void getProductsByStockStatusShouldReturn200()
            throws Exception {

        when(productService.getProductsByStockStatus(
                eq("LOW_STOCK"),
                any(Pageable.class)
        )).thenReturn(createProductPage());

        mockMvc.perform(get("/api/v1/products/stock-status")
                        .param("status", "LOW_STOCK"))
                .andExpect(status().isOk());

        verify(productService).getProductsByStockStatus(
                eq("LOW_STOCK"),
                any(Pageable.class)
        );
    }

    // 12. Filter products
    @Test
    void filterProductsShouldReturn200() throws Exception {

        when(productService.filterProducts(
                eq("Dell"),
                eq("Electronics"),
                eq("IN_STOCK"),
                any(Pageable.class)
        )).thenReturn(createProductPage());

        mockMvc.perform(get("/api/v1/products/filter")
                        .param("keyword", "Dell")
                        .param("category", "Electronics")
                        .param("status", "IN_STOCK"))
                .andExpect(status().isOk());

        verify(productService).filterProducts(
                eq("Dell"),
                eq("Electronics"),
                eq("IN_STOCK"),
                any(Pageable.class)
        );
    }
}