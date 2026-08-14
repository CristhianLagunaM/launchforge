CREATE TABLE discount_configuration (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    type VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL,
    percentage NUMERIC(5,2) NOT NULL,
    start_at TIMESTAMPTZ NULL,
    end_at TIMESTAMPTZ NULL,
    minimum_orders INTEGER NULL,
    lookback_months INTEGER NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NULL,
    CONSTRAINT uk_discount_configuration_code UNIQUE (code),
    CONSTRAINT chk_discount_configuration_percentage_range CHECK (percentage >= 0 AND percentage <= 100),
    CONSTRAINT chk_discount_configuration_minimum_orders_positive CHECK (minimum_orders IS NULL OR minimum_orders > 0),
    CONSTRAINT chk_discount_configuration_lookback_months_positive CHECK (lookback_months IS NULL OR lookback_months > 0),
    CONSTRAINT chk_discount_configuration_date_range CHECK (start_at IS NULL OR end_at IS NULL OR start_at <= end_at)
);

CREATE TABLE order_discounts (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    discount_configuration_id UUID NULL,
    code VARCHAR(80) NOT NULL,
    percentage NUMERIC(5,2) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    base_amount NUMERIC(19,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    application_order INTEGER NOT NULL,
    CONSTRAINT fk_order_discounts_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_discounts_discount_configuration FOREIGN KEY (discount_configuration_id) REFERENCES discount_configuration (id),
    CONSTRAINT chk_order_discounts_percentage_range CHECK (percentage >= 0 AND percentage <= 100),
    CONSTRAINT chk_order_discounts_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT chk_order_discounts_base_amount_non_negative CHECK (base_amount >= 0),
    CONSTRAINT chk_order_discounts_application_order_positive CHECK (application_order > 0)
);

INSERT INTO discount_configuration (
    id,
    code,
    type,
    enabled,
    percentage,
    start_at,
    end_at,
    minimum_orders,
    lookback_months,
    created_at,
    updated_at,
    updated_by
)
VALUES
    (
        '55555555-5555-5555-5555-555555555551',
        'TIME_RANGE',
        'TIME_RANGE',
        TRUE,
        10.00,
        '2026-08-01T00:00:00Z',
        '2026-08-31T23:59:59Z',
        NULL,
        NULL,
        '2026-01-01T00:00:00Z',
        '2026-01-01T00:00:00Z',
        NULL
    ),
    (
        '55555555-5555-5555-5555-555555555552',
        'RANDOM_ORDER',
        'RANDOM_ORDER',
        TRUE,
        50.00,
        NULL,
        NULL,
        NULL,
        NULL,
        '2026-01-01T00:00:00Z',
        '2026-01-01T00:00:00Z',
        NULL
    ),
    (
        '55555555-5555-5555-5555-555555555553',
        'FREQUENT_CUSTOMER',
        'FREQUENT_CUSTOMER',
        TRUE,
        5.00,
        NULL,
        NULL,
        5,
        12,
        '2026-01-01T00:00:00Z',
        '2026-01-01T00:00:00Z',
        NULL
    );
