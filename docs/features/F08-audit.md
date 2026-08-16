# Feature: auditoría técnica y funcional

## Alcance

- JPA Auditing;
- auditoría funcional transversal;
- correlation ID;
- consulta administrativa paginada.

## Dos niveles

### JPA Auditing

Completa campos técnicos como:

- `created_at`;
- `updated_at`;
- `created_by`;
- `updated_by`.

### Auditoría funcional

Registra eventos de negocio en `audit_log`.

```mermaid
flowchart LR
    UC[Use case] -->|@LogAction| A[AuditAspect]
    A --> W[AuditWriter]
    W --> DB[(audit_log)]
```

## Transacción

`AuditWriter` usa propagación `MANDATORY`.

Una acción exitosa y su auditoría confirman juntas. Si la transacción hace rollback, no queda evento de éxito.

## Acciones instrumentadas

Incluyen acciones como:

- `USER_CREATED`;
- `PRODUCT_CREATED`;
- `PRODUCT_UPDATED`;
- `PRODUCT_DISABLED`;
- `INVENTORY_ADJUSTED`;
- `ORDER_CREATED`;
- `ORDER_CONFIRMED`;
- `ORDER_COMPLETED`;
- `ORDER_CANCELLED`;
- `DISCOUNT_CONFIGURATION_UPDATED`.

`USER_STATUS_CHANGED` y `USER_ROLE_CHANGED` existen como conceptos de auditoría, pero el servicio administrativo actual debe verificarse/annotarse explícitamente antes de afirmar que esos dos cambios generan evento funcional.

## Correlation ID

`X-Correlation-Id`:

```text
[A-Za-z0-9._-]{1,100}
```

Inválido/ausente -> UUID.

## Metadata

Permitida:

- IDs/recurso;
- transición de estado;
- cantidades;
- datos pequeños específicos.

Prohibida:

- passwords;
- hashes;
- JWT;
- secretos;
- payload completo.

## Consulta

```text
GET /api/v1/audit
```

Filtros:

- action;
- resourceType;
- actor;
- from/to;
- page/size.

Solo `ADMIN`.
