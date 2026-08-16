# ADR: auditoría funcional transversal

## Status

Accepted

## Context

Las acciones mutables deben dejar trazabilidad homogénea sin mezclar persistencia de auditoría con reglas de catálogo, inventario, órdenes o descuentos.

## Decision

Se adopta `@LogAction` con Spring AOP.

El Aspect:

1. deja ejecutar el caso de uso;
2. registra únicamente resultados exitosos;
3. obtiene actor y contexto del request;
4. genera metadata mediante una lista permitida;
5. delega en `AuditWriter`.

`AuditWriter` exige una transacción existente con propagación `MANDATORY`. La transacción de negocio envuelve al Aspect, por lo que negocio y evento confirman o revierten juntos.

```mermaid
flowchart LR
    U[Use case] -->|@LogAction| A[AuditAspect]
    A --> M[Metadata allow-list]
    M --> W[AuditWriter MANDATORY]
    W --> DB[(audit_log)]
```

## Consequences

### Positivas

- contrato declarativo uniforme;
- menos llamadas repetidas;
- protección centralizada de datos;
- correlation ID e IP homogéneos;
- consistencia transaccional.

### Costos

- flujo menos visible que una llamada directa;
- el orden AOP/transacción debe estar cubierto por pruebas;
- expresiones de `resourceId` deben mantenerse simples.

## Alternatives

### Auditoría manual en cada service

Descartada porque aumenta duplicación, omisiones y acoplamiento.

## Límites

AOP no decide reglas de negocio ni estados. Solo registra el resultado.

GET ordinarios no se auditan y la metadata no serializa argumentos completos.
