UPDATE orders
SET discount_total = 5265.00,
    total = 2835.00,
    updated_at = '2026-08-10T09:05:00Z'
WHERE id = '44444444-4444-4444-4444-444444444406';

UPDATE order_discounts
SET amount = 4050.00,
    base_amount = 8100.00,
    reason = 'Seeded random-order winner calculated over the original subtotal.'
WHERE id = '77777777-7777-7777-7777-777777777705';

INSERT INTO order_discounts (
    id,
    order_id,
    discount_configuration_id,
    code,
    percentage,
    amount,
    base_amount,
    reason,
    application_order
)
VALUES (
    '77777777-7777-7777-7777-777777777707',
    '44444444-4444-4444-4444-444444444406',
    '55555555-5555-5555-5555-555555555553',
    'FREQUENT_CUSTOMER',
    5.00,
    405.00,
    8100.00,
    'Frequent customer threshold reached in previous confirmed/completed orders.',
    3
);

UPDATE audit_log
SET metadata = '{"discounts":["TIME_RANGE","RANDOM_ORDER","FREQUENT_CUSTOMER"],"total":"2835.00"}'::jsonb
WHERE id = '88888888-8888-8888-8888-888888888803';
