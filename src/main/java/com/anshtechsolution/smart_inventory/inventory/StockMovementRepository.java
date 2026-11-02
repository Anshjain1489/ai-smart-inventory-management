package com.anshtechsolution.smart_inventory.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    // Product history with pagination
    Page<StockMovement> findByProduct_IdOrderByCreatedAtDesc(
            Long productId,
            Pageable pageable
    );

    // Product history without pagination
    List<StockMovement> findByProduct_IdOrderByCreatedAtDesc(
            Long productId
    );

    // All stock movements without pagination
    List<StockMovement> findAllByOrderByCreatedAtDesc();

    // All stock movements with pagination
    Page<StockMovement> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    // Filter all movements by type with pagination
    Page<StockMovement> findByMovementTypeOrderByCreatedAtDesc(
            String movementType,
            Pageable pageable
    );

    // Filter product movements by type with pagination
    Page<StockMovement> findByProduct_IdAndMovementTypeOrderByCreatedAtDesc(
            Long productId,
            String movementType,
            Pageable pageable
    );

    // Idempotency checks
    Optional<StockMovement> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    // Total quantity received (IN)
    @Query("""
        SELECT COALESCE(SUM(sm.quantity), 0)
        FROM StockMovement sm
        WHERE sm.movementType = 'IN'
        """)
    Long getTotalStockIn();

    // Total quantity removed (OUT)
    @Query("""
        SELECT COALESCE(SUM(sm.quantity), 0)
        FROM StockMovement sm
        WHERE sm.movementType = 'OUT'
        """)
    Long getTotalStockOut();

    // Total IN transactions
    long countByMovementType(String movementType);

    // Total quantity received within a date range
    @Query("""
        SELECT COALESCE(SUM(sm.quantity), 0)
        FROM StockMovement sm
        WHERE sm.movementType = 'IN'
        AND sm.createdAt >= :startDate
        AND sm.createdAt <= :endDate
        """)
    Long getTotalStockInBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Total quantity removed within a date range
    @Query("""
        SELECT COALESCE(SUM(sm.quantity), 0)
        FROM StockMovement sm
        WHERE sm.movementType = 'OUT'
        AND sm.createdAt >= :startDate
        AND sm.createdAt <= :endDate
        """)
    Long getTotalStockOutBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    long countByMovementTypeAndCreatedAtBetween(
            String movementType,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
    SELECT COALESCE(SUM(sm.quantity), 0)
    FROM StockMovement sm
    WHERE sm.product.id = :productId
    AND sm.movementType = 'IN'
    """)
    Long getTotalStockInByProductId(@Param("productId") Long productId);


    @Query("""
    SELECT COALESCE(SUM(sm.quantity), 0)
    FROM StockMovement sm
    WHERE sm.product.id = :productId
    AND sm.movementType = 'OUT'
    """)
    Long getTotalStockOutByProductId(@Param("productId") Long productId);


    long countByProduct_IdAndMovementType(Long productId, String movementType);

    @Query("""
    SELECT COALESCE(SUM(sm.quantity), 0)
    FROM StockMovement sm
    WHERE LOWER(TRIM(sm.product.category)) = LOWER(TRIM(:category))
    AND sm.movementType = 'IN'
    """)
    Long getTotalStockInByCategory(@Param("category") String category);


    @Query("""
    SELECT COALESCE(SUM(sm.quantity), 0)
    FROM StockMovement sm
    WHERE LOWER(TRIM(sm.product.category)) = LOWER(TRIM(:category))
    AND sm.movementType = 'OUT'
    """)
    Long getTotalStockOutByCategory(@Param("category") String category);


//    long countByProduct_CategoryAndMovementType(
//            String category,
//            String movementType
//    );

    @Query("""
    SELECT COUNT(sm)
    FROM StockMovement sm
    WHERE LOWER(TRIM(sm.product.category)) = LOWER(TRIM(:category))
    AND sm.movementType = :movementType
    """)
    long countByProduct_CategoryAndMovementType(
            @Param("category") String category,
            @Param("movementType") String movementType
    );

    @Query("""
    SELECT COALESCE(SUM(sm.quantity), 0)
    FROM StockMovement sm
    WHERE sm.product.supplierId = :supplierId
    AND sm.movementType = 'IN'
    """)
    Long getTotalStockInBySupplierId(
            @Param("supplierId") Long supplierId
    );


    @Query("""
    SELECT COALESCE(SUM(sm.quantity), 0)
    FROM StockMovement sm
    WHERE sm.product.supplierId = :supplierId
    AND sm.movementType = 'OUT'
    """)
    Long getTotalStockOutBySupplierId(
            @Param("supplierId") Long supplierId
    );


    long countByProduct_SupplierIdAndMovementType(
            Long supplierId,
            String movementType
    );
}