package com.anshtechsolution.smart_inventory.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleItemRepository
        extends JpaRepository<SaleItem, Long> {

    @Query("""
        SELECT p.id AS productId,
               p.name AS productName,
               SUM(i.quantity) AS unitsSold
        FROM SaleItem i
        JOIN i.product p
        JOIN i.sale s
        WHERE s.status = 'COMPLETED'
          AND s.createdAt >= :start
          AND s.createdAt < :end
        GROUP BY p.id, p.name
        """)
    List<SalesVelocityProjection> findSalesVelocity(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}