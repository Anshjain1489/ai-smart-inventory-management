package com.anshtechsolution.smart_inventory.supplier;

import com.anshtechsolution.smart_inventory.supplier.dto.SupplierRequestDTO;
import com.anshtechsolution.smart_inventory.supplier.dto.SupplierResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierResponseDTO createSupplier(SupplierRequestDTO request) {

        validateEmail(request.getEmail(), null);

        Supplier supplier = new Supplier();
        supplier.setName(request.getName().trim());
        supplier.setEmail(normalizeEmail(request.getEmail()));
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());

        Supplier savedSupplier = supplierRepository.save(supplier);

        return mapToResponse(savedSupplier);
    }

    @Transactional(readOnly = true)
    public SupplierResponseDTO getSupplierById(Long id) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier not found with ID: " + id
                        )
                );

        return mapToResponse(supplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponseDTO> getAllSuppliers() {

        return supplierRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public SupplierResponseDTO updateSupplier(
            Long id,
            SupplierRequestDTO request
    ) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier not found with ID: " + id
                        )
                );

        validateEmail(request.getEmail(), id);

        supplier.setName(request.getName().trim());
        supplier.setEmail(normalizeEmail(request.getEmail()));
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());

        Supplier updatedSupplier = supplierRepository.save(supplier);

        return mapToResponse(updatedSupplier);
    }

    public void deleteSupplier(Long id) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier not found with ID: " + id
                        )
                );

        supplierRepository.delete(supplier);
    }

    private void validateEmail(String email, Long currentSupplierId) {

        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);

        supplierRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing ->
                        !existing.getId().equals(currentSupplierId)
                )
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A supplier with this email already exists"
                    );
                });
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }

    private SupplierResponseDTO mapToResponse(Supplier supplier) {

        SupplierResponseDTO response = new SupplierResponseDTO();

        response.setId(supplier.getId());
        response.setName(supplier.getName());
        response.setEmail(supplier.getEmail());
        response.setPhone(supplier.getPhone());
        response.setAddress(supplier.getAddress());
        response.setCreatedAt(supplier.getCreatedAt());

        return response;
    }
}