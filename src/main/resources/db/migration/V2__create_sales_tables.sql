
CREATE TABLE sales (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sale_number VARCHAR(50) NOT NULL UNIQUE,
    customer_name VARCHAR(150),
    total_amount DECIMAL(14, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_sales_total
        CHECK (total_amount >= 0)
);

CREATE TABLE sale_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sale_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    line_total DECIMAL(14, 2) NOT NULL,

    CONSTRAINT fk_sale_items_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id),

    CONSTRAINT fk_sale_items_product
        FOREIGN KEY (product_id) REFERENCES products(id),

    CONSTRAINT chk_sale_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_sale_items_price
        CHECK (unit_price >= 0),

    CONSTRAINT chk_sale_items_total
        CHECK (line_total >= 0)
);

CREATE INDEX idx_sales_created_at ON sales(created_at);
CREATE INDEX idx_sale_items_product ON sale_items(product_id);
CREATE INDEX idx_sale_items_sale ON sale_items(sale_id);
