# Feature: orders, transacciones e idempotencia

## 1. Alcance

Fase 5 implementa:

- creación de órdenes;
- recepción en estado pendiente y confirmación administrativa;
- consulta de órdenes;
- cancelación de órdenes confirmadas;
- protección transaccional sobre inventario;
- idempotencia para `POST /api/v1/orders`.

No incluye todavía descuentos.

## 2. Endpoints

Autenticados:

- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `PATCH /api/v1/orders/{id}/cancel`

Reglas:

- `CUSTOMER` puede crear y cancelar sus propias órdenes;
- `ADMIN` puede consultar órdenes;
- un cliente no puede consultar órdenes de otro cliente.

## 3. Flujo técnico

Creación:

`Angular checkout -> OrdersApiService -> OrderController -> CreateOrderUseCase -> reserva de Inventory + OrderRepository -> PostgreSQL`

La creación devuelve `CREATED` (Pendiente de confirmación). ADMIN revisa todas las órdenes en `GET /api/v1/orders/admin` y confirma con `PATCH /api/v1/orders/{id}/confirm`, momento en que la reserva se consume y la orden pasa a `CONFIRMED` (Confirmada). Después puede marcarla como `COMPLETED` mediante `PATCH /api/v1/orders/{id}/complete`. Solo una orden `CREATED` puede cancelarse; una orden confirmada o completada es definitiva y el backend rechaza cualquier cancelación, incluso desde una pantalla desactualizada.

Consulta:

`Angular orders page -> OrdersApiService -> OrderController -> OrderQueryService -> OrderRepository -> PostgreSQL`

Cancelación:

`Angular order detail -> OrdersApiService -> OrderController -> CancelOrderUseCase -> releaseReservation/restoreCapacity -> PostgreSQL`

## 4. Controller -> Application -> Repository

Clases principales:

- `OrderController`
- `CreateOrderUseCase`
- `TransactionalOrderCreator`
- `OrderQueryService`
- `CancelOrderUseCase`
- `OrderRepository`
- `InventoryManagementService`
- `OrderMapper`

Separación:

- controller: contrato HTTP y extracción del usuario autenticado;
- application: reglas de negocio, transacción e idempotencia;
- repository: carga y persistencia JPA;
- mapper: DTOs explícitos sin exponer entidades JPA.

## 5. Transacción de creación

La creación de una orden ocurre dentro de una transacción:

1. validar usuario y payload;
2. consolidar items repetidos;
3. cargar productos;
4. descontar capacidad por producto;
5. construir snapshots de `order_items`;
6. persistir `orders` y `order_items`;
7. confirmar la transacción.

Si algún paso falla:

- la orden no queda creada;
- la capacidad no queda descontada parcialmente;
- el cliente recibe `4xx` o `5xx` según el caso.

## 6. Idempotencia

`POST /api/v1/orders` acepta el header `Idempotency-Key`.

Comportamiento:

- si la misma llave ya creó una orden para el mismo cliente, el backend retorna esa orden;
- si dos requests concurrentes compiten con la misma llave, la restricción única en PostgreSQL evita duplicados;
- el caso de carrera se resuelve devolviendo la orden existente o `409` si todavía no puede recuperarse de forma consistente.

La unicidad se soporta con el índice parcial:

- `(customer_id, idempotency_key)` cuando `idempotency_key IS NOT NULL`

## 7. Snapshot de items

`order_items` conserva:

- `product_name`
- `sku`
- `unit_price`
- `subtotal`

Esto evita que una orden histórica cambie si luego se modifica el producto del catálogo.

## 8. Seguridad

- la seguridad real está en backend;
- Spring Security exige autenticación para `/api/v1/orders/**`;
- `@PreAuthorize("hasRole('CUSTOMER')")` protege creación y cancelación;
- la lectura valida ownership cuando el caller no es `ADMIN`.

## 9. Errores esperados

La API responde `application/problem+json`.

Casos comunes:

- `400` payload inválido o items vacíos;
- `401` sin JWT;
- `403` cliente intentando consultar una orden ajena;
- `404` orden, cliente o producto inexistente;
- `409` producto inactivo;
- `409` capacidad insuficiente;
- `409` orden ya cancelada;
- `409` conflicto de idempotencia.

## 10. Cómo probar la feature

Login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@launchforge.dev","password":"launchforge-demo"}'
```

Crear orden:

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <jwt>" \
  -H "Idempotency-Key: demo-order-001" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "22222222-2222-2222-2222-222222222221",
        "quantity": 1
      }
    ]
  }'
```

Listar órdenes:

```bash
curl -H "Authorization: Bearer <jwt>" \
  http://localhost:8080/api/v1/orders
```

Cancelar orden:

```bash
curl -X PATCH \
  -H "Authorization: Bearer <jwt>" \
  http://localhost:8080/api/v1/orders/<order-id>/cancel
```

## 11. Troubleshooting técnico

- si `POST /orders` devuelve `409`, revisar inventario e idempotency key;
- si el detalle no aparece, validar ownership y JWT;
- si una cancelación falla, revisar el `status` actual de la orden;
- si la UI reintenta el checkout, confirmar que reusa la misma `Idempotency-Key`;
- si hay diferencias entre catálogo actual y orden histórica, recordar que `order_items` usa snapshot deliberadamente.
