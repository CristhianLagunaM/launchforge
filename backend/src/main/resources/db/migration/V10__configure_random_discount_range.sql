UPDATE discount_configuration
SET start_at = '2026-08-01T00:00:00Z',
    end_at = '2026-08-31T23:59:59Z',
    updated_at = '2026-08-01T00:00:00Z'
WHERE code = 'RANDOM_ORDER'
  AND start_at IS NULL
  AND end_at IS NULL;
