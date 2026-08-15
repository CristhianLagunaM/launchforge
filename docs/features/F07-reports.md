# Feature: reportes administrativos

## Alcance

Fase 7 implementa tres reportes de solo lectura, protegidos con rol `ADMIN`:

- `GET /api/v1/reports/active-products`
- `GET /api/v1/reports/top-products`
- `GET /api/v1/reports/top-customers`

No incluye auditoría ni introduce el rol `AUDITOR`.

## Flujo técnico

`AdminReportsPageComponent -> ReportStore -> ReportApiService -> ReportController -> ReportQueryService -> ReportRepository -> PostgreSQL`

El controller aplica `@PreAuthorize("hasRole('ADMIN')")`. El guard Angular evita navegación improductiva, pero Spring Security es la frontera de autorización.

## Definición de reportes

### Productos activos

Lee `products JOIN categories`, filtra `products.active = true` y ordena por nombre y SKU. Devuelve `id`, `sku`, `name`, `category` y `price` mediante `ActiveProductProjection` y `ActiveProductReport`.

### Top 5 productos vendidos

```sql
SELECT p.id, p.sku, p.name, SUM(oi.quantity) AS quantity_sold
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
JOIN products p ON p.id = oi.product_id
WHERE o.status IN ('CONFIRMED', 'COMPLETED')
GROUP BY p.id, p.sku, p.name
ORDER BY quantity_sold DESC, p.name ASC, p.sku ASC
LIMIT 5;
```

`CANCELLED` y `CREATED` se excluyen en el `WHERE`. El desempate estable usa nombre y SKU ascendentes. La respuesta usa `TopProductProjection` y `TopProductReport`; el frontend recibe el ranking ya preparado.

### Top 5 clientes

```sql
SELECT u.id, u.email, u.first_name, u.last_name, COUNT(o.id) AS order_count
FROM orders o
JOIN users u ON u.id = o.customer_id
WHERE o.status IN ('CONFIRMED', 'COMPLETED')
GROUP BY u.id, u.email, u.first_name, u.last_name
ORDER BY order_count DESC, u.email ASC
LIMIT 5;
```

El desempate estable usa email ascendente. Se usan `TopCustomerProjection` y `TopCustomerReport`.

## Por qué SQL nativo y projections

Los rankings requieren `SUM`, `COUNT`, `GROUP BY`, orden y límite. SQL nativo expresa directamente esas operaciones PostgreSQL y permite `LIMIT 5` sin cargar entidades. Las interface projections materializan únicamente las columnas del contrato; no existe N+1 ni navegación lazy.

No se usa `findAll`, filtrado, grouping, sorting o limit en Java/Angular. El pequeño mapeo projection → record no realiza cálculos de reporte.

## Índices y plan observado

Las migraciones existentes ya aportan:

- `idx_orders_status`, usado para seleccionar `CONFIRMED/COMPLETED`;
- `idx_order_items_product_id`, útil al crecer la relación item/product;
- `idx_orders_customer_id`, útil al crecer la relación order/customer;
- PK de `products`, `users` y `orders`, usadas en joins.

`EXPLAIN (ANALYZE, BUFFERS)` se ejecutó sobre el seed local: 8 productos, 8 usuarios, 12 órdenes y 20 items. Resultado observado:

- activos: `Hash Join` y `Seq Scan` por cardinalidad pequeña; ~0.91 ms;
- top productos: `Bitmap Index Scan on idx_orders_status`, agregación y `LIMIT`; ~1.30 ms;
- top clientes: `Bitmap Index Scan on idx_orders_status`, agregación y `LIMIT`; ~0.58 ms.

Los `Seq Scan` sobre tablas diminutas son una decisión razonable del planner. No se agregó migración: la evidencia no justifica otro índice todavía. En producción debe repetirse `EXPLAIN ANALYZE` con cardinalidad representativa antes de diseñar un índice compuesto.

## Frontend

`/admin/reports` es una ruta lazy dentro del árbol protegido por `roleGuard` para `ADMIN`. `ReportStore` ejecuta las tres lecturas y mantiene estados `loading`, `error`, `empty` y `success`. Angular Material presenta tablas responsivas; no recalcula sumas ni ordena rankings.

## Pruebas

`ReportRepositoryIntegrationTest` crea datos controlados en PostgreSQL/Testcontainers y verifica activos, inactivos, suma, exclusión de canceladas, más de cinco candidatos, límite y empates. `ReportControllerMockMvcTest` cubre `ADMIN 200`, `CUSTOMER 403`, anónimo `401` y campos de respuesta. `report.store.spec.ts` cubre loading, mapping y error.

## Validación manual

1. Iniciar `docker compose up --build`.
2. Autenticar `admin@launchforge.dev` y abrir `/admin/reports`.
3. Comparar cada tabla con las consultas anteriores en `psql`.
4. Confirmar que un customer recibe `403` y una petición sin token recibe `401`.
5. Ejecutar `EXPLAIN (ANALYZE, BUFFERS)` antes de afirmar mejoras de índices.
