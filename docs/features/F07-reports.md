# Feature: reportes administrativos

## Alcance

Solo `ADMIN`:

- `GET /api/v1/reports/active-products`
- `GET /api/v1/reports/top-products`
- `GET /api/v1/reports/top-customers`
- `GET /api/v1/reports/dashboard`

## Flujo

```text
AdminReportsPage
 -> ReportStore
 -> ReportApiService
 -> ReportController
 -> ReportQueryService
 -> ReportRepository
 -> PostgreSQL
```

## Productos activos

Filtra:

```text
products.active = true
```

## Top 5 productos

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

## Top 5 clientes

```sql
SELECT u.id, u.email, u.first_name, u.last_name, COUNT(o.id) AS order_count
FROM orders o
JOIN users u ON u.id = o.customer_id
WHERE o.status IN ('CONFIRMED', 'COMPLETED')
GROUP BY u.id, u.email, u.first_name, u.last_name
ORDER BY order_count DESC, u.email ASC
LIMIT 5;
```

## Dashboard

Agrega en PostgreSQL:

- subtotal vendido;
- ingresos netos;
- descuentos;
- ticket promedio;
- conteo por estado;
- capacidad disponible;
- capacidad reservada;
- productos activos sin cupo;
- serie mensual de seis periodos.

Solo `CONFIRMED` y `COMPLETED` contribuyen a ventas/tendencias.

## SQL vs Java

Se usa SQL/projections porque `SUM`, `COUNT`, `GROUP BY`, filtros y `LIMIT` pertenecen al motor de base de datos.

No se hace `findAll()` para agrupar en memoria.

## Seguridad

- `ADMIN`: 200;
- `CUSTOMER`: 403;
- anónimo: 401.

## Validación manual

Crear datos reales desde la aplicación, confirmar/completar órdenes y comparar la API con consultas SQL.

No se depende de un admin demo.
