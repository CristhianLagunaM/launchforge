# ADR: discount engine strategy pipeline

## Status

Accepted

## Context

LaunchForge necesita aplicar varias reglas de descuento sobre una orden:

- descuentos por rango temporal;
- descuentos aleatorios;
- descuentos por cliente frecuente.

Las reglas deben ser:

- extensibles;
- testeables;
- auditables;
- configurables desde base de datos;
- compatibles con persistencia detallada en `order_discounts`.

## Decision

Se adopta un `DiscountEngine` que ejecuta un pipeline ordenado de `DiscountStrategy`.

Cada estrategia:

- declara su `DiscountCode`;
- decide `isApplicable(...)`;
- produce un `DiscountApplication` trazable;
- no consulta porcentajes hardcodeados.

La configuración activa se resuelve mediante `DiscountConfigurationService` y se comparte dentro de la ejecución del motor.

## Alternatives

### `if/else` gigante dentro de `OrderService`

Descartado porque:

- mezcla reglas, carga de configuración y persistencia;
- dificulta agregar nuevas reglas sin tocar lógica existente;
- complica pruebas unitarias enfocadas;
- reduce trazabilidad del cálculo.

### Estrategias instanciadas manualmente dentro del motor

Descartado porque rompe inversión de dependencias y vuelve difícil sustituir implementaciones en tests.

## Consequences

Positivas:

- cumple Open/Closed con cambios localizados por estrategia;
- permite tests deterministas para random;
- conserva trazabilidad por descuento aplicado;
- permite que varias reglas se acumulen sobre el subtotal original sin mezclar la lógica en `OrderService`.

Costos:

- hay más clases que en una solución ad hoc;
- requiere mantener una convención clara para `applicationOrder`;
- la validación de configuración debe ser estricta para evitar estados inconsistentes.
