# Security y JWT en LaunchForge

## Claims mínimos

Cada token incluye:

- `sub`: UUID del usuario
- `email`
- `roles`
- `iat`
- `exp`

## Por qué JWT

- elimina sesión de servidor
- escala mejor horizontalmente
- simplifica frontend SPA + API

## Por qué stateless

Cada request trae su contexto de autenticación. El backend no depende de `HttpSession`.

## Roles

- `ADMIN`
- `CUSTOMER`

Las autoridades Spring se proyectan como `ROLE_ADMIN` y `ROLE_CUSTOMER`.

## 401 vs 403

- `401`: token ausente, inválido o expirado
- `403`: token válido pero sin permisos suficientes

## Riesgos a evitar

- passwords sin hashing
- secretos versionados
- confiar solo en guards frontend
- guardar JWT o passwords en `audit_log.metadata`
