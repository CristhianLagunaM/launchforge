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

Notas de autorización:

- `CUSTOMER` puede crear y cancelar sus propias órdenes;
- `ADMIN` puede consultar órdenes;
- clientes autenticados no pueden consultar órdenes de otros clientes.

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
