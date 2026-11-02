package com.anshtechsolution.smart_inventory.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByEmailIgnoreCase(String email);

    List<Supplier> findByNameContainingIgnoreCase(String name);

    boolean existsByEmailIgnoreCase(String email);
}