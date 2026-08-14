CREATE TABLE inventory (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    available_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    version BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uk_inventory_product_id UNIQUE (product_id),
    CONSTRAINT chk_inventory_available_quantity_non_negative CHECK (available_quantity >= 0),
    CONSTRAINT chk_inventory_reserved_quantity_non_negative CHECK (reserved_quantity >= 0)
);
