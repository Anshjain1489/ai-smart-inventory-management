package com.anshtechsolution.smart_inventory.sales;

import com.anshtechsolution.smart_inventory.analytics.dto.DailySalesResponse;
import com.anshtechsolution.smart_inventory.analytics.dto.SalesSummaryResponse;
import com.anshtechsolution.smart_inventory.analytics.dto.TopProductResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
        SELECT new com.anshtechsolution.smart_inventory.analytics.dto.SalesSummaryResponse(
            COUNT(s.id),
            COALESCE(SUM(i.quantity), 0),
            COALESCE(SUM(i.lineTotal), 0)
        )
        FROM Sale s
        LEFT JOIN s.items i
        WHERE s.status = 'COMPLETED'
          AND s.createdAt >= :start
          AND s.createdAt < :end
        """)
    SalesSummaryResponse getSummary(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.anshtechsolution.smart_inventory.analytics.dto.TopProductResponse(
            p.id,
            p.name,
            SUM(i.quantity),
            SUM(i.lineTotal)
        )
        FROM SaleItem i
        JOIN i.product p
        JOIN i.sale s
        WHERE s.status = 'COMPLETED'
          AND s.createdAt >= :start
          AND s.createdAt < :end
        GROUP BY p.id, p.name
        ORDER BY SUM(i.quantity) DESC
        """)
    List<TopProductResponse> findTopProducts(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.anshtechsolution.smart_inventory.analytics.dto.DailySalesResponse(
            CAST(s.createdAt AS LocalDate),
            COUNT(DISTINCT s.id),
            SUM(s.totalAmount)
        )
        FROM Sale s
        WHERE s.status = 'COMPLETED'
          AND s.createdAt >= :start
          AND s.createdAt < :end
        GROUP BY CAST(s.createdAt AS LocalDate)
        ORDER BY CAST(s.createdAt AS LocalDate)
        """)
    List<DailySalesResponse> findDailySales(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}