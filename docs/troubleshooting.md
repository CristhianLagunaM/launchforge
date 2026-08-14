# Troubleshooting

## 1. `401 Unauthorized`

- verificar que el request admin incluya `Authorization: Bearer <jwt>`
- revisar expiración del token
- confirmar que el interceptor frontend siga registrado

## 2. `403 Forbidden`

- revisar roles dentro del JWT
- confirmar `ROLE_ADMIN` en backend
- probar el mismo endpoint con un token del admin demo

## 3. `404 Product not found`

- confirmar UUID correcto
- si el request es público, verificar que el producto siga activo
- revisar si la UI usa un id viejo después de editar/eliminar

## 4. `409 Conflict`

- consultar productos por `sku` o `slug`
- revisar si la edición intenta reutilizar un valor ya existente
- en inventario, revisar si la capacidad disponible alcanzaba
- si el mensaje habla de versión obsoleta, recargar la fila antes de reintentar

## 5. Búsqueda vacía o inesperada

- validar `minPrice <= maxPrice`
- revisar `category` por nombre o slug
- recordar que catálogo público fuerza `active=true`
- revisar `available=true` contra `inventory.available_quantity`

## 6. Docker Compose no levanta

- `docker compose ps`
- `docker compose logs backend`
- `docker compose logs frontend`
- `docker compose logs db`

Si cambiaste migraciones o seed, reinicio limpio local:

```bash
docker compose down -v
docker compose up --build
```

## 7. Conflicto optimista en inventario

- revisar `version` devuelta por `GET /api/v1/inventory/{productId}`
- confirmar que el `PATCH` envía esa misma versión
- si otra operación modificó la fila, el backend responderá `409`
- recargar inventario y reintentar con la versión nueva

## 8. Inventario insuficiente

- consultar `available_quantity` en PostgreSQL
- verificar si otro ajuste o consumo concurrente ya descontó capacidad
- revisar que no se esté intentando `DECREASE` por encima del disponible

## 9. Diagnóstico SQL rápido

```sql
SELECT
    p.id,
    p.sku,
    p.name,
    i.available_quantity,
    i.reserved_quantity,
    i.version,
    i.updated_at
FROM inventory i
JOIN products p ON p.id = i.product_id
ORDER BY p.name;
```

## 10. `409` al crear orden

- revisar `Idempotency-Key`
- revisar si el producto sigue activo
- consultar `inventory.available_quantity`
- verificar si el cliente reintentó un checkout anterior

## 11. No aparecen órdenes del cliente

- confirmar que el JWT pertenezca al usuario correcto
- revisar si la UI está consultando `/api/v1/orders` ya autenticada
- validar en backend que la orden se creó para ese `customer_id`

## 12. `403` al consultar detalle de orden

- un `CUSTOMER` solo puede ver órdenes propias
- probar el mismo id con un token `ADMIN`
- validar el `customer_id` de la orden en PostgreSQL

## 13. Idempotencia no evita duplicados

- confirmar que el cliente reusa exactamente la misma `Idempotency-Key`
- revisar la restricción única de `orders(customer_id, idempotency_key)`
- inspeccionar la tabla `flyway_schema_history` para confirmar que la migración base está aplicada
