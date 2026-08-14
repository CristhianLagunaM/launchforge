# ADR: inventory concurrency control

## Status

Accepted

## Context

LaunchForge vende capacidad operativa, no stock físico. Cuando varias operaciones intentan consumir el mismo cupo, existe riesgo de race condition y sobreasignación.

El backend debe funcionar en múltiples instancias, por lo que los locks en memoria no son una solución suficiente.

## Decision

Se adopta optimistic locking sobre `inventory` mediante la columna `version` y `@Version` en JPA.

Además:

- las invariantes viven dentro de `Inventory`
- `PATCH /api/v1/inventory/{productId}` exige la versión actual del cliente
- los conflictos se traducen a `409 Conflict`

## Consequences

Positivas:

- consistente con múltiples instancias backend
- sin infraestructura adicional
- simple de operar en PostgreSQL + JPA
- falla de forma explícita cuando una vista queda obsoleta

Costos:

- el cliente debe recargar y reintentar ante `409`
- en escenarios de altísima contención podría requerirse evaluar locking pesimista

## Alternatives considered

### `synchronized`

Descartado porque solo protege una JVM.

### Redis o locking distribuido

Descartado porque Fase 4 no lo justifica y agrega complejidad operativa innecesaria.

### Pessimistic locking por defecto

Descartado por ahora. Aumenta contención y complejidad sin evidencia de necesidad en este punto del proyecto.
