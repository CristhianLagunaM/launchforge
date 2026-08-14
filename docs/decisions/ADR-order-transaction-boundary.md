# ADR: order transaction boundary

## Status

Accepted

## Context

Crear una orden modifica más de una agregación persistente:

- `orders`
- `order_items`
- `inventory`

Si la operación no fuera atómica, podrían quedar órdenes creadas sin capacidad descontada o capacidad descontada sin orden persistida.

## Decision

La creación de órdenes se ejecuta dentro de una sola transacción de aplicación en `TransactionalOrderCreator`.

Dentro de esa transacción se hace:

1. validación de cliente;
2. consolidación de items;
3. carga de productos;
4. consumo de capacidad;
5. construcción de snapshots de items;
6. persistencia de orden e items.

La cancelación también se ejecuta en una transacción, restaurando capacidad y cambiando el estado de la orden de forma atómica.

## Consequences

Positivas:

- evita descuentos parciales de inventario;
- mantiene consistencia entre orden y capacidad;
- simplifica rollback ante errores.

Costos:

- la transacción toca varias filas y debe mantenerse corta;
- el diseño futuro de pagos o integraciones externas no debe ejecutarse dentro de esta misma transacción sin análisis adicional.

## Alternatives considered

### Crear primero la orden y luego ajustar inventario fuera de transacción

Descartado porque deja ventanas de inconsistencia.

### Coreografía asíncrona desde esta fase

Descartada porque Fase 5 aún no requiere orquestación distribuida.
