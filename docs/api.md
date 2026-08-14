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
