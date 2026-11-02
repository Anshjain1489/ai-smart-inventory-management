package com.anshtechsolution.smart_inventory.supplier;

import com.anshtechsolution.smart_inventory.supplier.dto.SupplierRequestDTO;
import com.anshtechsolution.smart_inventory.supplier.dto.SupplierResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier supplier;

    @BeforeEach
    void setUp() {

        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("ABC Suppliers");
        supplier.setEmail("abc@gmail.com");
        supplier.setPhone("9876543210");
        supplier.setAddress("Indore, Madhya Pradesh");
        supplier.setCreatedAt(LocalDateTime.now());
    }

    // Helper method to create request DTO
    private SupplierRequestDTO createRequest(
            String name,
            String email,
            String phone,
            String address) {

        SupplierRequestDTO request = new SupplierRequestDTO();

        request.setName(name);
        request.setEmail(email);
        request.setPhone(phone);
        request.setAddress(address);

        return request;
    }

    // 1. Create supplier successfully
    @Test
    void createSupplier_shouldCreateSupplierSuccessfully() {

        SupplierRequestDTO request = createRequest(
                "ABC Suppliers",
                "abc@gmail.com",
                "9876543210",
                "Indore, Madhya Pradesh"
        );

        when(supplierRepository.findByEmailIgnoreCase("abc@gmail.com"))
                .thenReturn(Optional.empty());

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier saved = invocation.getArgument(0);
                    saved.setId(1L);
                    saved.setCreatedAt(LocalDateTime.now());
                    return saved;
                });

        SupplierResponseDTO response =
                supplierService.createSupplier(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("ABC Suppliers");
        assertThat(response.getEmail()).isEqualTo("abc@gmail.com");
        assertThat(response.getPhone()).isEqualTo("9876543210");
        assertThat(response.getAddress())
                .isEqualTo("Indore, Madhya Pradesh");

        verify(supplierRepository).save(any(Supplier.class));
    }

    // 2. Trim name and normalize email
    @Test
    void createSupplier_shouldTrimNameAndNormalizeEmail() {

        SupplierRequestDTO request = createRequest(
                "  ABC Suppliers  ",
                "  ABC@GMAIL.COM  ",
                "9876543210",
                "Indore"
        );

        when(supplierRepository.findByEmailIgnoreCase("abc@gmail.com"))
                .thenReturn(Optional.empty());

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        SupplierResponseDTO response =
                supplierService.createSupplier(request);

        assertThat(response.getName()).isEqualTo("ABC Suppliers");
        assertThat(response.getEmail()).isEqualTo("abc@gmail.com");
    }

    // 3. Create supplier with blank email
    @Test
    void createSupplier_shouldAllowBlankEmail() {

        SupplierRequestDTO request = createRequest(
                "ABC Suppliers",
                " ",
                "9876543210",
                "Indore"
        );

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        SupplierResponseDTO response =
                supplierService.createSupplier(request);

        assertThat(response.getEmail()).isNull();

        verify(supplierRepository, never())
                .findByEmailIgnoreCase(anyString());

        verify(supplierRepository).save(any(Supplier.class));
    }

    // 4. Reject duplicate email during creation
    @Test
    void createSupplier_shouldRejectDuplicateEmail() {

        SupplierRequestDTO request = createRequest(
                "Another Supplier",
                "abc@gmail.com",
                "9999999999",
                "Bhopal"
        );

        when(supplierRepository.findByEmailIgnoreCase("abc@gmail.com"))
                .thenReturn(Optional.of(supplier));

        assertThatThrownBy(() ->
                supplierService.createSupplier(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "A supplier with this email already exists"
                );

        verify(supplierRepository, never())
                .save(any(Supplier.class));
    }

    // 5. Get supplier by ID
    @Test
    void getSupplierById_shouldReturnSupplier() {

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        SupplierResponseDTO response =
                supplierService.getSupplierById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("ABC Suppliers");
        assertThat(response.getEmail()).isEqualTo("abc@gmail.com");
    }

    // 6. Supplier not found by ID
    @Test
    void getSupplierById_shouldThrowWhenSupplierNotFound() {

        when(supplierRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                supplierService.getSupplierById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supplier not found with ID: 99");
    }

    // 7. Get all suppliers
    @Test
    void getAllSuppliers_shouldReturnAllSuppliers() {

        Supplier secondSupplier = new Supplier();
        secondSupplier.setId(2L);
        secondSupplier.setName("XYZ Suppliers");
        secondSupplier.setEmail("xyz@gmail.com");

        when(supplierRepository.findAll())
                .thenReturn(List.of(supplier, secondSupplier));

        List<SupplierResponseDTO> response =
                supplierService.getAllSuppliers();

        assertThat(response).hasSize(2);
        assertThat(response)
                .extracting(SupplierResponseDTO::getName)
                .containsExactly("ABC Suppliers", "XYZ Suppliers");
    }

    // 8. Update supplier successfully
    @Test
    void updateSupplier_shouldUpdateSupplierSuccessfully() {

        SupplierRequestDTO request = createRequest(
                "Updated Suppliers",
                "updated@gmail.com",
                "9999999999",
                "Bhopal"
        );

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.findByEmailIgnoreCase("updated@gmail.com"))
                .thenReturn(Optional.empty());

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponseDTO response =
                supplierService.updateSupplier(1L, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Updated Suppliers");
        assertThat(response.getEmail()).isEqualTo("updated@gmail.com");
        assertThat(response.getPhone()).isEqualTo("9999999999");
        assertThat(response.getAddress()).isEqualTo("Bhopal");

        verify(supplierRepository).save(supplier);
    }

    // 9. Allow supplier to retain its existing email
    @Test
    void updateSupplier_shouldAllowExistingSupplierEmail() {

        SupplierRequestDTO request = createRequest(
                "Updated ABC Suppliers",
                "abc@gmail.com",
                "9999999999",
                "Indore"
        );

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.findByEmailIgnoreCase("abc@gmail.com"))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponseDTO response =
                supplierService.updateSupplier(1L, request);

        assertThat(response.getName())
                .isEqualTo("Updated ABC Suppliers");

        assertThat(response.getEmail()).isEqualTo("abc@gmail.com");

        verify(supplierRepository).save(supplier);
    }

    // 10. Reject another supplier's email during update
    @Test
    void updateSupplier_shouldRejectEmailUsedByAnotherSupplier() {

        Supplier anotherSupplier = new Supplier();
        anotherSupplier.setId(2L);
        anotherSupplier.setEmail("xyz@gmail.com");

        SupplierRequestDTO request = createRequest(
                "Updated ABC Suppliers",
                "xyz@gmail.com",
                "9999999999",
                "Indore"
        );

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.findByEmailIgnoreCase("xyz@gmail.com"))
                .thenReturn(Optional.of(anotherSupplier));

        assertThatThrownBy(() ->
                supplierService.updateSupplier(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "A supplier with this email already exists"
                );

        verify(supplierRepository, never())
                .save(any(Supplier.class));
    }

    // 11. Delete supplier successfully
    @Test
    void deleteSupplier_shouldDeleteSupplierSuccessfully() {

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        supplierService.deleteSupplier(1L);

        verify(supplierRepository).delete(supplier);
    }

    // 12. Delete supplier that does not exist
    @Test
    void deleteSupplier_shouldThrowWhenSupplierNotFound() {

        when(supplierRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                supplierService.deleteSupplier(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supplier not found with ID: 99");

        verify(supplierRepository, never())
                .delete(any(Supplier.class));
    }
}