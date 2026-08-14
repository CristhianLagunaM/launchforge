CREATE INDEX idx_products_active ON products (active);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_name ON products (name);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_customer_id_created_at ON orders (customer_id, created_at);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

CREATE INDEX idx_order_discounts_order_id ON order_discounts (order_id);
CREATE INDEX idx_order_discounts_code ON order_discounts (code);

CREATE INDEX idx_audit_log_actor_user_id ON audit_log (actor_user_id);
CREATE INDEX idx_audit_log_action ON audit_log (action);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
CREATE INDEX idx_audit_log_resource_type_resource_id ON audit_log (resource_type, resource_id);
