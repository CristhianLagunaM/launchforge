# ADR: idempotency for order creation

## Status

Accepted

## Context

Un checkout puede repetirse por doble clic, retry, refresh o timeout.

Sin idempotencia podría crear órdenes duplicadas y reservar capacidad dos veces.

## Decision

`POST /api/v1/orders` acepta `Idempotency-Key`.

La llave se evalúa por cliente:

```text
(customer_id, idempotency_key)
```

PostgreSQL impone unicidad mediante índice parcial.

Si la orden ya existe, el backend retorna la existente.

## Consequences

### Positivas

- retry seguro;
- evita duplicados;
- protege inventario.

### Costos

- frontend debe conservar la llave mientras la intención no cambie;
- backend debe resolver carreras de inserción.

## Alternatives

### Sin idempotencia

Descartado por riesgo de duplicados.

### Hash automático del payload

Descartado porque payloads similares no necesariamente representan la misma intención.
