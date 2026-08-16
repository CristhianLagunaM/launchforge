# Feature: discount engine

## Alcance

- estrategias extensibles;
- configuración en DB;
- persistencia en `order_discounts`;
- administración de configuración;
- desglose en la orden.

Reglas:

- `TIME_RANGE`;
- `RANDOM_ORDER`;
- `FREQUENT_CUSTOMER`.

## Flujo

```text
POST /orders
 -> CreateOrderUseCase
 -> TransactionalOrderCreator
 -> DiscountEngine
 -> DiscountStrategy[]
 -> order_discounts
```

Los descuentos se calculan **durante la creación de la orden**, no durante su confirmación.

## Configuración ADMIN

- `GET /api/v1/discount-configurations`
- `PATCH /api/v1/discount-configurations/{code}`

## Orden de trazabilidad

1. `TIME_RANGE`;
2. `RANDOM_ORDER`;
3. `FREQUENT_CUSTOMER`.

Cada descuento utiliza el subtotal original.

Ejemplo:

```text
subtotal = 100

TIME_RANGE        10% -> 10
RANDOM_ORDER      50% -> 50
FREQUENT_CUSTOMER  5% -> 5

discount_total = 65
total = 35
```

## Frequent customer

Se cuentan únicamente:

```text
CONFIRMED
COMPLETED
```

dentro del `lookback`.

`CREATED` y `CANCELLED` no cuentan.

## Random

`RandomProvider` desacopla la aleatoriedad y permite tests deterministas.

## Persistencia histórica

Cada aplicación guarda:

- code;
- percentage;
- base amount;
- amount;
- reason;
- application order.

Cambiar `discount_configuration` no modifica órdenes existentes.

## Configuración inicial

`V15` deja las tres reglas deshabilitadas inicialmente.

Para pruebas manuales, un `ADMIN` debe habilitarlas y configurar los parámetros necesarios.

No se depende de `frequent@...` ni de cuentas demo.

## Rounding

`BigDecimal`, escala 2 y `HALF_UP`.

## Troubleshooting

- revisar `enabled`;
- revisar rango UTC;
- contar órdenes válidas del cliente;
- revisar `RandomProvider`;
- comprobar que `base_amount` corresponda al subtotal original.
