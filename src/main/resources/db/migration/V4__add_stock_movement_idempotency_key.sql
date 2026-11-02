ALTER TABLE stock_movements
ADD COLUMN idempotency_key VARCHAR(100) NULL;

CREATE UNIQUE INDEX uk_stock_movements_idempotency_key
ON stock_movements (idempotency_key);