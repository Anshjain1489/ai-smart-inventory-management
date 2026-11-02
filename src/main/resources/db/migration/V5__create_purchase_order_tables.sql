
CREATE TABLE purchase_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    order_date DATETIME(6) NOT NULL,
    expected_delivery_date DATETIME(6),
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    notes VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_purchase_orders_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id)
        ON DELETE RESTRICT
);

CREATE TABLE purchase_order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    ordered_quantity INT NOT NULL,
    received_quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(12, 2) NOT NULL,

    CONSTRAINT fk_purchase_order_items_order
        FOREIGN KEY (purchase_order_id)
        REFERENCES purchase_orders(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_purchase_order_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_ordered_quantity
        CHECK (ordered_quantity > 0),

    CONSTRAINT chk_received_quantity
        CHECK (
            received_quantity >= 0
            AND received_quantity <= ordered_quantity
        ),

    CONSTRAINT chk_purchase_unit_price
        CHECK (unit_price >= 0)
);

CREATE INDEX idx_purchase_orders_supplier
    ON purchase_orders(supplier_id);

CREATE INDEX idx_purchase_orders_status
    ON purchase_orders(status);

CREATE INDEX idx_purchase_order_items_order
    ON purchase_order_items(purchase_order_id);

CREATE INDEX idx_purchase_order_items_product
    ON purchase_order_items(product_id);
