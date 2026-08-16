# Feature: discount engine

## 1. Alcance

Fase 6 implementa:

- motor extensible de descuentos basado en estrategias;
- aplicación acumulable de descuentos sobre el subtotal original;
- persistencia trazable en `order_discounts`;
- configuración editable por `ADMIN`;
- visualización del desglose en detalle de orden;
- reglas `TIME_RANGE`, `RANDOM_ORDER` y `FREQUENT_CUSTOMER`.

No incluye reportes ni auditoría adicional.

## 2. Flujo funcional

`Angular checkout -> OrdersApiService -> OrderController -> CreateOrderUseCase -> TransactionalOrderCreator -> DiscountEngine -> DiscountStrategy[] -> PostgreSQL`

## 3. Endpoints

Órdenes autenticadas:

- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `PATCH /api/v1/orders/{id}/cancel`

Configuración admin:

- `GET /api/v1/discount-configurations`
- `PATCH /api/v1/discount-configurations/{code}`

`PATCH` body:

```json
{
  "enabled": true,
  "percentage": 10.00,
  "startAt": "2026-08-01T00:00:00Z",
  "endAt": "2026-08-31T23:59:59Z",
  "minimumOrders": null,
  "lookbackMonths": null
}
```

## 4. Clases principales

- `DiscountContext`
- `DiscountStrategy`
- `DiscountEngine`
- `DiscountConfigurationService`
- `TimeRangeDiscountStrategy`
- `RandomOrderDiscountStrategy`
- `FrequentCustomerDiscountStrategy`
- `RandomProvider`
- `DiscountConfigurationController`
- `TransactionalOrderCreator`
- `OrderMapper`

## 5. Controller -> Application -> Repository

- controller: `OrderController`, `DiscountConfigurationController`
- application: `CreateOrderUseCase`, `TransactionalOrderCreator`, `DiscountEngine`, estrategias
- repository: `OrderRepository`, `DiscountConfigurationRepository`
- persistence: `CustomerOrder`, `OrderDiscount`, `DiscountConfiguration`

## 6. Búsqueda y carga de configuración

`DiscountConfigurationService` carga las configuraciones activas una sola vez por ejecución del motor y las entrega indexadas por `DiscountCode`.

Esto evita:

- queries duplicadas por estrategia;
- porcentajes hardcodeados;
- lógica dispersa en controllers o services de órdenes.

## 7. Orden de aplicación

El pipeline se ordena por `applicationOrder`:

1. `TIME_RANGE`
2. `RANDOM_ORDER`
3. `FREQUENT_CUSTOMER`

El cálculo conserva orden de trazabilidad. Los descuentos combinables son lineales acumulativos: cada porcentaje se calcula sobre el subtotal original y luego se suman los importes. Por ejemplo, 10% + 50% + 5% = 65%; sobre US$950 el descuento es US$617,50 y el total US$332,50:

- subtotal `100.00`
- `TIME_RANGE` 10% => `10.00`
- `RANDOM_ORDER` 50% => `50.00`
- `FREQUENT_CUSTOMER` 5% => `5.00`

Resultado:

- `discount_total = 65.00`
- `total = 35.00`

## 8. DTO vs Entity

- la API no expone entidades JPA;
- `OrderResponse` incluye un arreglo `discounts`;
- `DiscountConfigurationView` y `DiscountConfigurationUpdateRequest` aíslan el contrato admin.

## 9. Persistencia y trazabilidad

Cada descuento aplicado se persiste en `order_discounts` con:

- `code`
- `percentage`
- `base_amount`
- `amount`
- `reason`
- `application_order`

La orden conserva agregados:

- `orders.discount_total`
- `orders.total`

Una orden histórica no cambia si después se modifica `discount_configuration`.

## 10. Frequent customer

La elegibilidad se calcula con `COUNT(*)` filtrando por:

- `customer_id`
- `status IN (CONFIRMED, COMPLETED)`
- `created_at >= lookback`

`CANCELLED` no cuenta.

## 11. Random determinista en tests

Producción usa `RandomProvider`.

Tests pueden inyectar una implementación fija para forzar:

- orden ganadora;
- orden perdedora.

Esto evita `new Random()` dentro de la estrategia y mantiene pruebas repetibles.

## 12. Rounding

El backend usa `BigDecimal` con escala `2` y `RoundingMode.HALF_UP` en:

- cálculo de cada descuento;
- `discount_total`;
- `total`.

El frontend solo renderiza valores calculados por backend.

## 13. Seguridad

- backend sigue siendo la frontera real;
- `POST /api/v1/orders` requiere `CUSTOMER`;
- `GET/PATCH /api/v1/discount-configurations/**` requiere `ADMIN`.

## 14. Errores esperados

- `400` configuración inválida enviada por admin
- `401` sin JWT
- `403` rol insuficiente
- `404` orden o configuración inexistente
- `409` total negativo por configuración inválida
- `409` conflicto de inventario o idempotencia durante creación

## 15. Cómo probar la feature

1. autenticar `frequent@launchforge.dev`;
2. crear una orden en agosto 2026 o con rango activo en configuración;
3. consultar el detalle de la orden;
4. verificar `discountTotal`, `total` y arreglo `discounts`;
5. autenticar `admin@launchforge.dev`;
6. abrir `/admin/discounts` y editar una regla;
7. crear otra orden y confirmar el nuevo comportamiento.

SQL recomendado:

```sql
SELECT
    code,
    percentage,
    base_amount,
    amount,
    application_order
FROM order_discounts
WHERE order_id = '<order-id>'::uuid
ORDER BY application_order;
```

## 16. Troubleshooting técnico

- si falta un descuento, revisar `enabled`, rango y porcentaje en `discount_configuration`;
- si `FREQUENT_CUSTOMER` no aplica, contar órdenes válidas del usuario y excluir `CANCELLED`;
- si `RANDOM_ORDER` no es testeable, verificar qué `RandomProvider` se inyectó;
- si el total no cuadra, revisar que cada descuento tome `subtotal` como `base_amount`;
- si una orden vieja cambió visualmente, inspeccionar `order_discounts` y no `discount_configuration`.
