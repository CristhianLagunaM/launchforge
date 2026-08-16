INSERT INTO inventory (
    id,
    product_id,
    available_quantity,
    reserved_quantity,
    version,
    updated_at
)
SELECT
    gen_random_uuid(),
    p.id,
    0,
    0,
    0,
    NOW()
FROM products p
WHERE NOT EXISTS (
    SELECT 1
    FROM inventory i
    WHERE i.product_id = p.id
);