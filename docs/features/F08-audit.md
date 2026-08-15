# Feature: auditoría técnica y funcional

## Alcance

Fase 8 implementa JPA Auditing, auditoría funcional transversal, correlation ID y consulta administrativa paginada. No incluye Quality/CI/Hardening ni endpoints para modificar el historial.

## Dos niveles de auditoría

JPA Auditing completa created_at, updated_at, created_by y updated_by en entidades compatibles. AuditorAware usa el subject UUID del JWT. Registro público, Flyway y procesos técnicos no inventan un usuario: el actor permanece NULL cuando el esquema lo permite.

La auditoría funcional registra acciones de negocio en audit_log: actor, acción, recurso, resource ID, correlation ID, IP, metadata pequeña y fecha.

## Flujo técnico y transacción

Caso de uso con Transactional y LogAction → AuditAspect → AuditMetadataFactory → AuditWriter(MANDATORY) → audit_log.

El Aspect ejecuta primero el método. Solo si termina correctamente resuelve el recurso, construye metadata permitida y escribe el evento. La transacción Spring envuelve al Aspect; negocio y evento confirman o revierten juntos. Esta fase no persiste intentos fallidos de seguridad.

## Acciones

- USER_CREATED
- PRODUCT_CREATED, PRODUCT_UPDATED y PRODUCT_DISABLED
- INVENTORY_ADJUSTED
- ORDER_CREATED y ORDER_CANCELLED
- DISCOUNT_CONFIGURATION_UPDATED

USER_STATUS_CHANGED y USER_ROLE_CHANGED quedan definidos para futuros casos administrativos; esos casos de uso todavía no existen. No se auditan GET ordinarios.

## Correlation ID e IP

CorrelationIdFilter acepta X-Correlation-Id solo cuando cumple [A-Za-z0-9._-]{1,100}; en otro caso genera UUID. Lo devuelve en la respuesta, lo añade a MDC y lo expone al Aspect durante el request.

Se registra request.getRemoteAddr(). No se confía en X-Forwarded-For: solo debe usarse detrás de un reverse proxy conocido y configurado.

## Metadata y exclusiones

JSONB contiene únicamente detalles pequeños: SKU/estado, cantidades anterior/nueva, transición de orden y configuración de descuento. No almacena snapshots completos, passwords, hashes, JWT, secretos ni payloads arbitrarios.

## Consulta administrativa

GET /api/v1/audit requiere ADMIN, ordena por createdAt DESC y acepta action, resourceType, actor, from, to, page y size. Actor admite UUID o email. `/admin/audit` ofrece filtros, paginación y estados loading/error/empty, sin mutaciones.

## Persistencia y pruebas

No se creó migración: V6 ya contiene las columnas y V7 los índices requeridos. Las pruebas PostgreSQL/MockMvc verifican producto, inventario, cancelación, actor, correlation ID, metadata, rollback, filtros, paginación y autorización. El store Angular cubre carga y error.
