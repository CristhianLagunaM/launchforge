# ADR: auditoría funcional transversal

## Status

Accepted

## Context

Las acciones mutables deben dejar trazabilidad homogénea sin mezclar persistencia de auditoría con reglas de catálogo, inventario, órdenes o descuentos.

## Decision

Se adopta LogAction con Spring AOP. El Aspect registra exclusivamente resultados exitosos, obtiene contexto seguro, genera metadata mediante una lista permitida y delega en AuditWriter.

AuditWriter exige una transacción existente con propagación MANDATORY. La transacción de negocio envuelve al Aspect, por lo que negocio y evento confirman o revierten juntos.

## AOP transversal

Ventajas:

- contrato declarativo uniforme;
- elimina llamadas repetidas;
- centraliza protección de datos, correlation ID e IP;
- mantiene una semántica transaccional.

Costos:

- flujo menos visible que una llamada directa;
- el orden de advisors AOP/transacción debe probarse;
- las expresiones de resource ID deben ser simples y testeadas.

## Llamadas manuales a AuditService

Son explícitas y personalizables, pero repiten código, facilitan omisiones y mezclan una preocupación transversal con negocio.

## Límites

AOP no decide reglas ni estados. Solo registra el resultado calculado. Los GET comunes no se auditan y la metadata nunca serializa argumentos completos.
