# Security

LaunchForge usa Spring Security stateless, JWT HS256 y autorización backend con PreAuthorize. Angular guards mejoran navegación, pero no sustituyen la frontera backend.

## Auditoría

GET /api/v1/audit requiere ROLE_ADMIN y es de solo lectura. El actor se obtiene del subject del JWT validado. Sin autenticación —registro público, seed Flyway o proceso técnico— actor_user_id permanece NULL; nunca se crea un usuario ficticio.

## Datos prohibidos

Logs y metadata excluyen passwords, password_hash, JWT completo, JWT secret, payloads completos y datos sensibles innecesarios.

## Correlation ID e IP

X-Correlation-Id se valida y limita a 100 caracteres; valores ausentes o inválidos se reemplazan por UUID. La IP procede de la conexión servlet. X-Forwarded-For no se usa porque solo es confiable detrás de proxies autorizados.
