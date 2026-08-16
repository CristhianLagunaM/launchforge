# ADR: inventory concurrency control

## Status

Accepted

## Context

LaunchForge vende capacidad operativa. Varias operaciones concurrentes pueden intentar reservar o ajustar el mismo cupo.

Locks en memoria no son suficientes para múltiples instancias.

## Decision

Se adopta optimistic locking sobre `inventory` mediante:

```java
@Version
private Long version;
```

`PATCH /api/v1/inventory/{productId}` recibe la versión conocida por el cliente.

Los conflictos se traducen a `409 Conflict`.

## Consequences

### Positivas

- funciona con múltiples instancias;
- sin infraestructura adicional;
- integrado con JPA/PostgreSQL;
- detecta explícitamente vistas obsoletas.

### Costos

- cliente debe recargar/reintentar tras `409`;
- alta contención podría justificar locking pesimista futuro.

## Alternatives

### `synchronized`

Descartado: solo protege una JVM.

### Redis/lock distribuido

Descartado: complejidad no justificada.

### Pessimistic locking por defecto

Descartado mientras no exista evidencia de alta contención.
