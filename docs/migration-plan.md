# LaunchForge — Plan inicial de migraciones Flyway

## Objetivo

Este documento define el orden de migraciones iniciales.

Ruta:

```text
backend/src/main/resources/db/migration/
```

## Migraciones

### V1__create_identity.sql

Crea:

```text
users
roles
user_roles
```

Incluye:

```text
PK
FK
UNIQUE users.email
UNIQUE roles.name
seed ADMIN
seed CUSTOMER
```

### V2__create_catalog.sql

Crea:

```text
categories
products
```

Incluye:

```text
UNIQUE category.name
UNIQUE category.slug
UNIQUE product.sku
UNIQUE product.slug
CHECK product.price >= 0
FK product.category_id -> categories.id
```

### V3__create_inventory.sql

Crea:

```text
inventory
```

Incluye:

```text
UNIQUE product_id
CHECK available_quantity >= 0
CHECK reserved_quantity >= 0
version
FK inventory.product_id -> products.id
```

### V4__create_orders.sql

Crea:

```text
orders
order_items
```

Incluye:

```text
UNIQUE order_number
CHECK monetary values >= 0
CHECK quantity > 0
partial UNIQUE(customer_id, idempotency_key)
CHECK status IN (CREATED, CONFIRMED, CANCELLED, COMPLETED)
```

### V5__create_discounts.sql

Crea:

```text
discount_configuration
order_discounts
```

Seed:

```text
TIME_RANGE = 10%
RANDOM_ORDER = 50%
FREQUENT_CUSTOMER = 5%
```

Incluye:

```text
UNIQUE discount_configuration.code
CHECK percentage between 0 and 100
CHECK minimum_orders > 0 when present
CHECK lookback_months > 0 when present
CHECK start_at <= end_at when both exist
```

### V6__create_audit.sql

Crea:

```text
audit_log
```

Incluye:

```text
FK actor_user_id -> users.id
metadata JSONB
```

### V7__create_indexes.sql

Índices para:

```text
products.active
products.category_id
products.name
orders.customer_id
orders.created_at
orders.status
orders(customer_id, created_at)
order_items.order_id
order_items.product_id
order_discounts.order_id
order_discounts.code
audit_log.actor_user_id
audit_log.action
audit_log.created_at
audit_log(resource_type, resource_id)
```

### V8__seed_demo_data.sql

Crea:

```text
usuarios demo
productos demo
inventario
órdenes históricas
order_items
order_discounts
audit_log mínimo
datos necesarios para Top 5
cliente frecuente
```

Notas:

```text
Passwords demo almacenadas con BCrypt
Fechas seed deterministas en UTC
Usuario frecuente con >= 5 órdenes CONFIRMED/COMPLETED en los últimos 12 meses
```

## Regla

Después de aplicar V1-V8 y compartirlas:

```text
NO EDITARLAS
```

Los siguientes cambios deben ser:

```text
V9__...
V10__...
V11__...
```

No corregir historia editando scripts ya aplicados. Corregir con nueva migración incremental.

### V11__align_discount_seed_with_accumulative_rules.sql

Alinea los datos demo históricos con la regla acumulable sobre subtotal original:

```text
ajusta orders.discount_total y orders.total
ajusta RANDOM_ORDER en order_discounts
agrega FREQUENT_CUSTOMER a la orden demo principal
actualiza metadata de audit_log asociada
```

### V9__create_product_search_indexes.sql

Índices adicionales para catálogo:

```text
products.price
inventory.available_quantity
```

## Verificación

Después de cada migración ejecutar:

```sql
SELECT *
FROM flyway_schema_history
ORDER BY installed_rank;
```

Y validar desde cero:

```bash
docker compose down -v
docker compose up --build
```
## V12 — Reconciliación de reservas

`V12__reconcile_inventory_reservations.sql` normaliza `reserved_quantity` y lo reconstruye exclusivamente a partir de órdenes `CREATED`. Así, una base existente no conserva reservas huérfanas después de cambiar el flujo a pendiente → confirmada.
