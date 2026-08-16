UPDATE inventory SET reserved_quantity = 0;

UPDATE inventory i
SET reserved_quantity = pending.quantity
FROM (
    SELECT oi.product_id, SUM(oi.quantity)::INTEGER AS quantity
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE o.status = 'CREATED'
    GROUP BY oi.product_id
) pending
WHERE i.product_id = pending.product_id;
