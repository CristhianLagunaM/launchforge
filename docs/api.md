# API

La API utiliza el prefijo `/api/v1`. Swagger se encuentra disponible en `/swagger-ui/index.html` y el contrato OpenAPI en `/v3/api-docs`.

## Authentication

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

El registro crea usuarios con rol `CUSTOMER`. La creación inicial del primer `ADMIN` se realiza mediante el procedimiento de bootstrap documentado en el README; la administración posterior se hace mediante los endpoints administrativos.

## Admin users

Solo `ADMIN`:

- `GET /api/v1/admin/users`
- `PATCH /api/v1/admin/users/{id}`

El `PATCH` permite cambiar:

- estado `enabled`;
- rol (`ADMIN` o `CUSTOMER`).

No permite modificar contraseña, hash, nombre o correo.

## Categories

- `GET /api/v1/categories`

Devuelve categorías activas para usuarios anónimos. Un `ADMIN` autenticado puede recibir categorías inactivas cuando corresponda a la lógica del backend.

## Products

### Público

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`

### Administración de productos

- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `PATCH /api/v1/products/{id}/status`
- `DELETE /api/v1/products/{id}`

### Query params soportados

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

## Inventory

Solo `ADMIN`:

- `GET /api/v1/inventory`
- `GET /api/v1/inventory/{productId}`
- `PATCH /api/v1/inventory/{productId}`

Ejemplo de `PATCH`:

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

### CUSTOMER

- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `PATCH /api/v1/orders/{id}/cancel`

### Administración de órdenes

- `GET /api/v1/orders/admin`
- `GET /api/v1/orders/{id}`
- `PATCH /api/v1/orders/{id}/cancel`
- `PATCH /api/v1/orders/{id}/confirm`
- `PATCH /api/v1/orders/{id}/complete`

Reglas:

- `CUSTOMER` crea órdenes y consulta únicamente las propias;
- `ADMIN` puede consultar todas;
- solo órdenes `CREATED` pueden cancelarse;
- `CREATED -> CONFIRMED` requiere `ADMIN`;
- `CONFIRMED -> COMPLETED` requiere `ADMIN`.

### Crear orden

`POST /api/v1/orders` acepta opcionalmente:

```text
Idempotency-Key: <value>
```

Body actual:

```json
{
  "items": [
    {
      "productId": "22222222-2222-2222-2222-222222222221",
      "quantity": 1
    }
  ],
  "requirementDescription": "Necesito una plataforma web para gestionar solicitudes.",
  "projectObjective": "Centralizar el proceso y reducir tiempos operativos.",
  "contactEmail": "cliente@ejemplo.com",
  "contactPhone": "+57 3000000000",
  "desiredDeliveryDate": "2026-10-30",
  "referencesUrl": "https://example.com/reference"
}
```

Validaciones relevantes:

- `items`: obligatorio y no vacío;
- `requirementDescription`: obligatorio, máximo 3000 caracteres;
- `projectObjective`: obligatorio, máximo 1000;
- `contactEmail`: obligatorio, email válido, máximo 180;
- `contactPhone`: opcional, máximo 40;
- `desiredDeliveryDate`: opcional;
- `referencesUrl`: opcional, máximo 2000.

La respuesta contiene, entre otros:

- `subtotal`;
- `discountTotal`;
- `total`;
- `items[]`;
- `discounts[]`.

Cada descuento incluye información como:

- `code`;
- `percentage`;
- `baseAmount`;
- `amount`;
- `reason`;
- `applicationOrder`.

## Discount configuration

Solo `ADMIN`:

- `GET /api/v1/discount-configurations`
- `PATCH /api/v1/discount-configurations/{code}`

Casos de uso:

- habilitar/deshabilitar una regla;
- ajustar porcentaje;
- configurar `startAt/endAt`;
- configurar `minimumOrders/lookbackMonths`.

Reglas disponibles:

- `TIME_RANGE`;
- `RANDOM_ORDER`;
- `FREQUENT_CUSTOMER`.

## Reports

Solo `ADMIN`:

- `GET /api/v1/reports/active-products`
- `GET /api/v1/reports/top-products`
- `GET /api/v1/reports/top-customers`
- `GET /api/v1/reports/dashboard`

`dashboard` consolida métricas financieras, estados de órdenes, capacidad operativa y evolución mensual.

Los rankings consideran únicamente órdenes:

```text
CONFIRMED
COMPLETED
```

y excluyen:

```text
CREATED
CANCELLED
```

Top productos limita a cinco filas y usa desempate estable por nombre/SKU. Top clientes usa conteo de órdenes y desempate por email.

## Audit

Solo `ADMIN`:

- `GET /api/v1/audit`

Filtros opcionales:

- `action`
- `resourceType`
- `actor`
- `from`
- `to`
- `page`
- `size`

La respuesta es paginada, ordenada por fecha descendente y de solo lectura.

## Correlation ID

Las respuestas propagan `X-Correlation-Id`.

Un valor proporcionado por el cliente debe cumplir:

```text
[A-Za-z0-9._-]{1,100}
```

Si falta o es inválido, el backend genera un UUID.

## Errores

La API utiliza `application/problem+json`.

Casos habituales:

- `400`: validación o filtros/configuración inválidos;
- `401`: autenticación ausente o inválida;
- `403`: rol insuficiente o acceso a recurso ajeno;
- `404`: recurso inexistente;
- `409`: SKU/slug duplicado;
- `409`: inventario insuficiente;
- `409`: conflicto optimista de inventario;
- `409`: conflicto de idempotencia;
- `409`: transición de orden no permitida.
