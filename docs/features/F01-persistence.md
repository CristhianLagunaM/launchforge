# Feature: persistencia base y migraciones Flyway

## 1. Qué problema resuelve

Entrega el primer modelo persistente completo de LaunchForge y fija la frontera de verdad: PostgreSQL + Flyway + JPA `validate`.

## 2. Alcance real

Incluye migraciones `V1` a `V8`, entidades JPA para identidad, catálogo, inventario, órdenes, descuentos y auditoría, seed demo determinista y tests de integración con Testcontainers.

No incluye login, JWT, controllers funcionales, CRUD de productos, flujo de órdenes ni motor de descuentos.

## 3. Tablas creadas

- `users`, `roles`, `user_roles`
- `categories`, `products`
- `inventory`
- `orders`, `order_items`
- `discount_configuration`, `order_discounts`
- `audit_log`

## 4. Decisiones claves

- UUID en recursos principales; `roles` y `categories` quedan secuenciales por ser catálogos internos.
- `NUMERIC(19,2)` en SQL y `BigDecimal` en Java para dinero.
- `TIMESTAMPTZ` en PostgreSQL e `Instant` en backend, siempre en UTC.
- `orders.status` se persiste como `STRING`, no ordinal.
- `inventory.version` implementa optimistic locking.
- `order_items` preserva snapshot comercial mínimo con `product_name`, `sku` y `unit_price`.
- `audit_log.metadata` usa `JSONB` porque su estructura es variable por evento.

## 5. Seed demo

Incluye:

- 1 admin
- 1 customer base
- 1 frequent customer
- 5 clientes adicionales
- catálogo LaunchForge con categorías y productos activos
- inventario operativo
- órdenes históricas suficientes para futuros reportes Top 5
- descuentos aplicados y auditoría mínima

Todas las passwords demo usan BCrypt. El valor en texto para pruebas manuales puede documentarse fuera de la DB; en persistencia nunca queda texto plano.

## 6. Validaciones y constraints

DB protege invariantes con `UNIQUE`, `FK` y `CHECK`. Bean Validation replica reglas razonables en entidades para fallar antes de llegar al motor.

Ejemplos críticos:

- email único
- precio no negativo
- inventario no negativo
- cantidad de item positiva
- descuentos y totales no negativos
- `discount_total <= subtotal`
- `status` dentro del enum persistido
- `(customer_id, idempotency_key)` único cuando la key existe

## 7. Índices

Además de PK/UK, se crean índices para consultas previstas:

- `products(active)`, `products(category_id)`, `products(name)`
- `orders(customer_id)`, `orders(created_at)`, `orders(status)`, `orders(customer_id, created_at)`
- `order_items(order_id)`, `order_items(product_id)`
- `order_discounts(order_id)`, `order_discounts(code)`
- `audit_log(actor_user_id)`, `audit_log(action)`, `audit_log(created_at)`, `audit_log(resource_type, resource_id)`

## 8. Pruebas

Los tests de integración levantan PostgreSQL real con Testcontainers y validan:

- ejecución completa de Flyway
- compatibilidad esquema/JPA vía arranque Spring con `ddl-auto=validate`
- constraints importantes
- relaciones JPA principales
- incremento de `version` en `inventory`

## 9. Cómo depurarlo

1. Revisar `flyway_schema_history`.
2. Comparar excepción SQL con la migración que introdujo el objeto.
3. Si falla `validate`, alinear entidad y migración; no cambiar a `update`.
4. Si falla un seed, reconstruir local con `docker compose down -v` y volver a subir.

## 10. Regla de evolución

Una vez compartidas `V1-V8`, no se editan. Después de `V9`, cualquier cambio nuevo entra como `V10__...` o la siguiente versión libre.
