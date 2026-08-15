# API

## Authentication

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

## Categories

- `GET /api/v1/categories`

Devuelve categorías activas para usuarios anónimos. Un admin autenticado puede recibir también categorías inactivas cuando aplique la lógica del backend.

## Products

### Público

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`

### ADMIN

- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `PATCH /api/v1/products/{id}/status`
- `DELETE /api/v1/products/{id}`

## Inventory

### ADMIN

- `GET /api/v1/inventory`
- `GET /api/v1/inventory/{productId}`
- `PATCH /api/v1/inventory/{productId}`

`PATCH` body:

```json
{
  "operation": "INCREASE",
  "quantity": 2,
  "version": 0
}
```

Sort fields soportados:

- `productName`
- `sku`
- `availableQuantity`
- `reservedQuantity`
- `version`
- `updatedAt`

## Orders

Autenticados:

- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `PATCH /api/v1/orders/{id}/cancel`

`POST /api/v1/orders` acepta el header opcional `Idempotency-Key`.

Body:

```json
{
  "items": [
    {
      "productId": "22222222-2222-2222-2222-222222222221",
      "quantity": 1
    }
  ]
}
```

La respuesta de detalle incluye:

- `subtotal`
- `discountTotal`
- `total`
- `items[]`
- `discounts[]` con `code`, `percentage`, `baseAmount`, `amount`, `reason`, `applicationOrder`

## Discount configuration

### ADMIN

- `GET /api/v1/discount-configurations`
- `PATCH /api/v1/discount-configurations/{code}`

Casos de uso:

- habilitar/deshabilitar una regla;
- ajustar porcentaje;
- ajustar `startAt/endAt`;
- ajustar `minimumOrders/lookbackMonths`.

Notas de autorización:

- `CUSTOMER` puede crear y cancelar sus propias órdenes;
- `ADMIN` puede consultar órdenes;
- clientes autenticados no pueden consultar órdenes de otros clientes.

## Reports

Solo `ADMIN`:

- `GET /api/v1/reports/active-products`
- `GET /api/v1/reports/top-products`
- `GET /api/v1/reports/top-customers`

Los rankings incluyen únicamente órdenes `CONFIRMED` y `COMPLETED`; excluyen `CANCELLED` y `CREATED`. Top productos devuelve máximo cinco filas ordenadas por `quantitySold DESC`, con desempate `name/sku ASC`. Top clientes usa `orderCount DESC` y email ascendente como desempate.

El backend devuelve DTOs preparados; el frontend no calcula sumas ni rankings.

El frontend consume rutas relativas `/api/v1`; Nginx las reenvía al backend en Compose. El cliente interpreta Problem Details y diferencia especialmente 401, 403, validación y conflictos 409.

## Audit

Solo ADMIN:

- GET /api/v1/audit

Filtros opcionales: action, resourceType, actor (UUID o email), from, to, page y size. La respuesta es paginada, ordenada por createdAt descendente y de solo lectura. Todas las respuestas propagan X-Correlation-Id; un valor enviado por el cliente debe tener máximo 100 caracteres y usar letras, números, punto, guion o guion bajo.

## Query params soportados en catálogo

- `name`
- `sku`
- `category`
- `minPrice`
- `maxPrice`
- `active`
- `available`
- `page`
- `size`
- `sort`

Ejemplo:

```text
GET /api/v1/products?name=web&category=WEB&minPrice=100&maxPrice=5000&active=true&page=0&size=20&sort=name,asc
```

## Errores

La API responde `application/problem+json`.

Casos esperados:

- `400` validación o filtros inválidos
- `401` sin autenticación
- `403` sin rol suficiente
- `404` recurso inexistente
- `409` conflicto de SKU/slug
- `409` inventario insuficiente o conflicto optimista de inventario
- `409` orden ya cancelada, producto inactivo o conflicto de idempotencia
- `409` configuración inválida que produciría un cálculo inconsistente
