package com.anshtechsolution.smart_inventory.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderItemRepository
        extends JpaRepository<PurchaseOrderItem, Long> {

    // Find all items belonging to a purchase order
    List<PurchaseOrderItem> findByPurchaseOrder_Id(Long purchaseOrderId);

    // Find items associated with a particular product
    List<PurchaseOrderItem> findByProduct_Id(Long productId);
}