package com.anshtechsolution.smart_inventory.product;

import com.anshtechsolution.smart_inventory.report.dto.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("""
        SELECT p FROM Product p
        WHERE p.quantity <= p.reorderLevel
        ORDER BY p.quantity ASC
        """)
    Page<Product> findLowStockProducts(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("""
    SELECT p FROM Product p
    WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Product> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    @Query("""
    SELECT p FROM Product p
    WHERE (
        LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    AND LOWER(p.category) = LOWER(:category)
    """)
    Page<Product> searchByKeywordAndCategory(
            @Param("keyword") String keyword,
            @Param("category") String category,
            Pageable pageable
    );

    Page<Product> findByQuantity(Integer quantity, Pageable pageable);

    @Query("""
    SELECT p FROM Product p
    WHERE p.quantity > 0
      AND p.quantity <= p.reorderLevel
    ORDER BY p.quantity ASC
    """)
    Page<Product> findProductsWithLowStock(Pageable pageable);

    @Query("""
    SELECT p FROM Product p
    WHERE p.quantity > p.reorderLevel
    ORDER BY p.quantity ASC
    """)
    Page<Product> findProductsInStock(Pageable pageable);

    @Query("""
    SELECT p FROM Product p
    WHERE (
        :keyword IS NULL
        OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    AND (
        :category IS NULL
        OR LOWER(p.category) = LOWER(:category)
    )
    AND (
        :status IS NULL
        OR (:status = 'OUT_OF_STOCK' AND p.quantity = 0)
        OR (:status = 'LOW_STOCK'
            AND p.quantity > 0
            AND p.quantity <= p.reorderLevel)
        OR (:status = 'IN_STOCK'
            AND p.quantity > p.reorderLevel)
    )
    """)
    Page<Product> filterProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
    SELECT new com.anshtechsolution.smart_inventory.report.dto.InventorySummaryReportDTO(
        COUNT(p),
        COALESCE(SUM(p.quantity), 0L),
        COALESCE(SUM(p.quantity * p.unitPrice), 0),
        COUNT(DISTINCT p.category)
    )
    FROM Product p
    """)
    InventorySummaryReportDTO getInventorySummary();


    @Query("""
    SELECT new com.anshtechsolution.smart_inventory.report.dto.StockStatusReportDTO(
        COUNT(CASE WHEN p.quantity > p.reorderLevel THEN p.id END),
        COUNT(CASE WHEN p.quantity > 0 AND p.quantity <= p.reorderLevel THEN p.id END),
        COUNT(CASE WHEN p.quantity = 0 THEN p.id END),
        COUNT(p)
    )
    FROM Product p
    """)
    StockStatusReportDTO getStockStatusReport();

    @Query("""
    SELECT new com.anshtechsolution.smart_inventory.report.dto.CategoryInventoryReportDTO(
        p.category,
        COUNT(p),
        COALESCE(SUM(p.quantity), 0L),
        COALESCE(SUM(p.quantity * p.unitPrice), 0)
    )
    FROM Product p
    WHERE p.category IS NOT NULL
    GROUP BY p.category
    ORDER BY p.category ASC
    """)
    List<CategoryInventoryReportDTO> getCategoryInventoryReport();

    @Query("""
    SELECT new com.anshtechsolution.smart_inventory.report.dto.InventoryValuationItemDTO(
        p.id,
        p.sku,
        p.name,
        p.category,
        p.quantity,
        p.unitPrice,
        p.quantity * p.unitPrice
    )
    FROM Product p
    ORDER BY p.name ASC
    """)
    List<InventoryValuationItemDTO> getInventoryValuationItems();

    @Query("""
    SELECT new com.anshtechsolution.smart_inventory.report.dto.DateFilteredInventoryReportDTO(
        COUNT(p),
        COALESCE(SUM(p.quantity), 0L),
        COALESCE(SUM(p.quantity * p.unitPrice), 0)
    )
    FROM Product p
    WHERE p.createdAt >= :startDate
      AND p.createdAt < :endDate
    """)
    DateFilteredInventoryReportDTO getInventoryReportByDate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT p FROM Product p
    WHERE (
        :keyword IS NULL
        OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    AND (
        :category IS NULL
        OR LOWER(p.category) = LOWER(:category)
    )
    AND (
        :status IS NULL
        OR (:status = 'OUT_OF_STOCK' AND p.quantity = 0)
        OR (:status = 'LOW_STOCK'
            AND p.quantity > 0
            AND p.quantity <= p.reorderLevel)
        OR (:status = 'IN_STOCK'
            AND p.quantity > p.reorderLevel)
    )
    ORDER BY p.name ASC
    """)
    List<Product> findProductsForExport(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") String status
    );

    long countByCategoryIgnoreCase(String category);

    long countBySupplierId(Long supplierId);


    @Query("""
    SELECT COUNT(p)
    FROM Product p
    """)
    Long countTotalProducts();


    @Query("""
    SELECT COALESCE(SUM(p.quantity), 0)
    FROM Product p
    """)
    Long getTotalInventoryQuantity();


    @Query("""
    SELECT COUNT(p)
    FROM Product p
    WHERE p.quantity <= p.reorderLevel
    """)
    Long countLowStockProducts();
}