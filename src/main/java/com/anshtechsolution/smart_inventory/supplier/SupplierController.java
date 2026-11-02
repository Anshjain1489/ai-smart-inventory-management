package com.anshtechsolution.smart_inventory.supplier;

import com.anshtechsolution.smart_inventory.supplier.dto.SupplierRequestDTO;
import com.anshtechsolution.smart_inventory.supplier.dto.SupplierResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<SupplierResponseDTO> createSupplier(
            @Valid @RequestBody SupplierRequestDTO request
    ) {
        SupplierResponseDTO response =
                supplierService.createSupplier(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponseDTO>> getAllSuppliers() {

        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> getSupplierById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDTO request
    ) {
        return ResponseEntity.ok(
                supplierService.updateSupplier(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable Long id
    ) {
        supplierService.deleteSupplier(id);

        return ResponseEntity.noContent().build();
    }
}