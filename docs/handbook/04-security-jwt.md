# Security y JWT en LaunchForge

## Claims

- `sub`;
- `email`;
- `roles`;
- `iat`;
- `exp`.

## Stateless

Cada request contiene su contexto mediante JWT. El backend no depende de `HttpSession`.

## Roles

```text
ADMIN
CUSTOMER
```

Spring:

```text
ROLE_ADMIN
ROLE_CUSTOMER
```

## Registro y bootstrap

Registro -> `CUSTOMER`.

Primer `ADMIN`:

```text
registrar -> user_roles ADMIN -> nuevo login
```

Administración posterior desde `/api/v1/admin/users`.

## 401 vs 403

- 401: token inválido/ausente/expirado;
- 403: token válido sin autoridad o acceso permitido.

## Riesgos

Evitar:

- password sin hashing;
- secretos versionados;
- confiar solo en guards frontend;
- JWT/password en auditoría;
- crear hashes manuales en DB para bootstrap.
