# Troubleshooting

## 1. Diagnóstico general

```bash
docker compose config
docker compose ps
docker compose logs db
docker compose logs backend
docker compose logs frontend
```

Con `.env.example`:

```text
Frontend:   http://localhost:8088
Backend:    http://localhost:8080
PostgreSQL: localhost:55432
```

Dentro de Compose:

```text
backend -> db:5432
frontend/nginx -> backend:8080
```

## 2. Reset local

Si necesitas reconstruir la base de desarrollo:

```bash
docker compose down -v
docker compose up --build
```

> `down -v` elimina los datos locales de PostgreSQL.

## 3. CI y análisis

Backend:

```bash
cd backend
mvn clean verify
```

Frontend:

```bash
cd frontend
npm ci
npm run lint
npm test -- --watch=false
npm run build
```

Testcontainers requiere Docker activo.

## 4. `401 Unauthorized`

- revisar `Authorization: Bearer <jwt>`;
- verificar expiración;
- comprobar `JWT_SECRET`;
- revisar interceptor frontend;
- iniciar sesión nuevamente si el rol cambió después de emitir el token.

## 5. `403 Forbidden`

- inspeccionar roles del JWT;
- confirmar `ROLE_ADMIN`/`ROLE_CUSTOMER`;
- revisar `@PreAuthorize`;
- comprobar ownership para órdenes.

### Primer ADMIN

Si todavía no existe administrador, seguir el bootstrap del README:

```text
registro CUSTOMER -> asignar ADMIN en user_roles -> nuevo login
```

## 6. PostgreSQL no conecta

```bash
docker compose ps
docker compose logs db
docker compose exec db pg_isready -U launchforge -d launchforge
```

Con `.env.example`, desde el host PostgreSQL está publicado en `55432`; dentro de Compose el backend siempre usa `db:5432`.

## 7. Flyway falla

Consultar:

```sql
SELECT *
FROM flyway_schema_history
ORDER BY installed_rank;
```

No:

- editar una migración ya aplicada;
- deshabilitar Flyway;
- cambiar Hibernate a `ddl-auto=update`.

Corregir mediante una nueva migración.

## 8. Hibernate `validate` falla

Comparar:

- entidad JPA;
- última migración;
- nombre/tipo/nullability de columna.

La solución es alinear modelo y esquema, no habilitar generación automática.

## 9. `404 Product not found`

- revisar UUID;
- verificar `active`;
- comprobar si la UI conserva un ID anterior.

## 10. `409` de catálogo

- revisar SKU;
- revisar slug;
- comprobar duplicados antes de reintentar.

## 11. Inventario insuficiente

Consultar:

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

## 12. Conflicto optimista

- consultar versión vigente;
- recargar la fila;
- reintentar con nueva versión.

No ocultar el `409` con reintentos infinitos.

## 13. `409` al crear orden

Revisar:

- producto activo;
- capacidad disponible;
- `Idempotency-Key`;
- si la misma intención ya creó una orden.

## 14. Cancelación rechazada

Solo `CREATED` puede cancelarse.

```text
CREATED   -> CANCELLED : válido
CONFIRMED -> CANCELLED : inválido
COMPLETED -> CANCELLED : inválido
```

Al cancelar `CREATED`, se libera la reserva.

## 15. Requerimientos de orden rechazados

`POST /orders` exige:

- `requirementDescription`;
- `projectObjective`;
- `contactEmail`;
- al menos un item.

Revisar límites definidos en `CreateOrderRequest`.

## 16. Descuento no aplicado

- revisar `discount_configuration.enabled`;
- revisar rango UTC;
- revisar porcentaje;
- para frecuente, contar solo `CONFIRMED/COMPLETED`;
- revisar `minimum_orders` y `lookback_months`;
- consultar `order_discounts`.

## 17. Total no coincide

Cada regla se calcula sobre el subtotal original.

Comparar:

```sql
SELECT
    code,
    percentage,
    base_amount,
    amount,
    application_order
FROM order_discounts
WHERE order_id = '<uuid>'
ORDER BY application_order;
```

La suma de `amount` debe corresponder a `orders.discount_total`.

## 18. Orden histórica cambia visualmente

La fuente histórica es:

```text
order_items
order_discounts
```

No se debe recalcular con el precio ni la configuración actual.

## 19. Idempotencia no evita duplicados

- confirmar que el frontend reutiliza exactamente la misma llave;
- verificar la restricción por `(customer_id, idempotency_key)`;
- comprobar que cambiar el carrito genere una intención nueva.

## 20. Reportes vacíos

- confirmar existencia de `CONFIRMED`/`COMPLETED`;
- `CREATED` y `CANCELLED` no cuentan en rankings;
- comprobar rol `ADMIN`.

## 21. Reportes lentos

Usar:

```sql
EXPLAIN (ANALYZE, BUFFERS)
...
```

No añadir índices sin medir cardinalidad y plan.

## 22. Auditoría ausente

- comprobar que la operación terminó con commit;
- revisar `@LogAction`;
- buscar por `correlation_id`;
- recordar que una operación fallida no genera evento de éxito.

## 23. Correlation ID

Debe cumplir:

```text
[A-Za-z0-9._-]{1,100}
```

Valores inválidos se sustituyen por UUID.

## 24. Frontend: peer dependencies

No usar como solución permanente:

```text
--force
--legacy-peer-deps
```

La combinación actual es Angular 21, NgRx Signals 21 y TypeScript 5.9.

## 25. Puerto ocupado

Si aparece `failed to bind host port`:

- cambiar el puerto en `.env`; o
- liberar el puerto en el sistema operativo.

Después:

```bash
docker compose config
docker compose up --build
```
