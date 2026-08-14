CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    subtotal NUMERIC(19,2) NOT NULL,
    discount_total NUMERIC(19,2) NOT NULL,
    total NUMERIC(19,2) NOT NULL,
    idempotency_key VARCHAR(120) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT chk_orders_status CHECK (status IN ('CREATED', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_orders_subtotal_non_negative CHECK (subtotal >= 0),
    CONSTRAINT chk_orders_discount_total_non_negative CHECK (discount_total >= 0),
    CONSTRAINT chk_orders_total_non_negative CHECK (total >= 0),
    CONSTRAINT chk_orders_discount_total_le_subtotal CHECK (discount_total <= subtotal)
);

CREATE UNIQUE INDEX ux_orders_customer_idempotency_key_not_null
    ON orders (customer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(180) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    subtotal NUMERIC(19,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_subtotal_non_negative CHECK (subtotal >= 0)
);
