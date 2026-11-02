package com.anshtechsolution.smart_inventory.inventory;

import com.anshtechsolution.smart_inventory.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "movement_type", nullable = false, length = 20)
    private String movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String notes;


    @org.hibernate.annotations.Generated
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

}