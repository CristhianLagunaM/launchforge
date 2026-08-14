# ADR: idempotency for order creation

## Status

Accepted

## Context

El checkout puede reenviar la misma petición por:

- doble clic;
- refresh;
- reintento del navegador;
- timeouts entre frontend y backend.

Sin idempotencia, esos reintentos podrían crear órdenes duplicadas y consumir capacidad más de una vez.

## Decision

`POST /api/v1/orders` acepta `Idempotency-Key`.

La llave se evalúa por cliente:

- si ya existe una orden para `(customer_id, idempotency_key)`, se retorna la misma orden;
- PostgreSQL impone unicidad con un índice parcial;
- el caso concurrente se resuelve consultando la orden creada por la petición competidora.

## Consequences

Positivas:

- checkout tolera reintentos seguros;
- evita órdenes duplicadas por reenvíos del cliente;
- reduce riesgo de descontar capacidad dos veces por la misma intención.

Costos:

- el cliente debe preservar la llave durante el intento de checkout;
- el backend debe manejar explícitamente la carrera de inserción.

## Alternatives considered

### Sin idempotencia

Descartado por riesgo de duplicados.

### Hash implícito del payload

Descartado porque distintos payloads similares no representan necesariamente la misma intención del usuario.
