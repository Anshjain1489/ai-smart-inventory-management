package com.anshtechsolution.smart_inventory.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    // Find purchase orders belonging to a specific supplier
    List<PurchaseOrder> findBySupplier_Id(Long supplierId);

    // Find purchase orders by their status
    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);
}