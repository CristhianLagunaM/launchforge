# ADR: order transaction boundary

## Status

Accepted

## Context

Crear una orden modifica:

- `orders`;
- `order_items`;
- `inventory`;
- `order_discounts` cuando aplican reglas.

Una operación parcial puede dejar capacidad reservada sin orden o una orden sin reserva consistente.

## Decision

La creación se ejecuta dentro de una única transacción de aplicación en `TransactionalOrderCreator`.

Dentro de ella:

1. valida cliente y request;
2. consolida items;
3. carga productos;
4. reserva capacidad;
5. construye snapshots;
6. calcula descuentos;
7. persiste orden, items y descuentos.

La cancelación de una orden `CREATED` también es transaccional:

1. carga la orden;
2. valida ownership/rol;
3. valida estado;
4. libera reservas;
5. cambia a `CANCELLED`;
6. persiste.

La confirmación y el completado tienen sus propias fronteras transaccionales.

## Consequences

### Positivas

- rollback consistente;
- evita reservas parciales;
- protege relación orden/inventario.

### Costos

- transacción toca varias filas;
- debe mantenerse corta;
- integraciones externas futuras no deberían agregarse sin analizar la frontera.

## Alternatives

### Orden e inventario en transacciones separadas

Descartado por ventanas de inconsistencia.

### Coreografía asíncrona

Descartada porque el dominio actual no requiere arquitectura distribuida.
