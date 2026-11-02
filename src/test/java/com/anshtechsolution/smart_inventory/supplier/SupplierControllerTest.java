package com.anshtechsolution.smart_inventory.supplier;

import com.anshtechsolution.smart_inventory.supplier.dto.SupplierRequestDTO;
import com.anshtechsolution.smart_inventory.supplier.dto.SupplierResponseDTO;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupplierController.class)
@AutoConfigureMockMvc(addFilters = false)
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService supplierService;

    private SupplierResponseDTO createSupplierResponse() {

        SupplierResponseDTO response = new SupplierResponseDTO();

        response.setId(1L);
        response.setName("ABC Suppliers");
        response.setEmail("abc@example.com");
        response.setPhone("9876543210");
        response.setAddress("Indore, Madhya Pradesh");
        response.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

        return response;
    }

    // 1. Create supplier
    @Test
    void createSupplierShouldReturn201() throws Exception {

        when(supplierService.createSupplier(
                any(SupplierRequestDTO.class)
        )).thenReturn(createSupplierResponse());

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "ABC Suppliers",
                                  "email": "abc@example.com",
                                  "phone": "9876543210",
                                  "address": "Indore, Madhya Pradesh"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("ABC Suppliers"))
                .andExpect(jsonPath("$.email")
                        .value("abc@example.com"))
                .andExpect(jsonPath("$.phone").value("9876543210"));

        verify(supplierService).createSupplier(
                any(SupplierRequestDTO.class)
        );
    }

    // 2. Reject blank supplier name
    @Test
    void createSupplierShouldReturn400WhenNameIsBlank()
            throws Exception {

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "abc@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(supplierService);
    }

    // 3. Reject invalid email
    @Test
    void createSupplierShouldReturn400WhenEmailIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "ABC Suppliers",
                                  "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(supplierService);
    }

    // 4. Reject supplier name exceeding 150 characters
    @Test
    void createSupplierShouldReturn400WhenNameIsTooLong()
            throws Exception {

        String longName = "A".repeat(151);

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "abc@example.com"
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(supplierService);
    }

    // 5. Get all suppliers
    @Test
    void getAllSuppliersShouldReturn200() throws Exception {

        when(supplierService.getAllSuppliers())
                .thenReturn(List.of(createSupplierResponse()));

        mockMvc.perform(get("/api/v1/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("ABC Suppliers"));

        verify(supplierService).getAllSuppliers();
    }

    // 6. Get supplier by ID
    @Test
    void getSupplierByIdShouldReturn200() throws Exception {

        when(supplierService.getSupplierById(1L))
                .thenReturn(createSupplierResponse());

        mockMvc.perform(get("/api/v1/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email")
                        .value("abc@example.com"));

        verify(supplierService).getSupplierById(1L);
    }

    // 7. Update supplier
    @Test
    void updateSupplierShouldReturn200() throws Exception {

        when(supplierService.updateSupplier(
                eq(1L),
                any(SupplierRequestDTO.class)
        )).thenReturn(createSupplierResponse());

        mockMvc.perform(put("/api/v1/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "ABC Suppliers",
                                  "email": "abc@example.com",
                                  "phone": "9876543210",
                                  "address": "Updated Indore Address"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("ABC Suppliers"));

        verify(supplierService).updateSupplier(
                eq(1L),
                any(SupplierRequestDTO.class)
        );
    }

    // 8. Reject invalid update request
    @Test
    void updateSupplierShouldReturn400WhenRequestIsInvalid()
            throws Exception {

        mockMvc.perform(put("/api/v1/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(supplierService);
    }

    // 9. Delete supplier
    @Test
    void deleteSupplierShouldReturn204() throws Exception {

        doNothing().when(supplierService).deleteSupplier(1L);

        mockMvc.perform(delete("/api/v1/suppliers/1"))
                .andExpect(status().isNoContent());

        verify(supplierService).deleteSupplier(1L);
    }
}