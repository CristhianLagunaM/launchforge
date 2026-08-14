# Feature: authentication and authorization

## 1. Qué problema resuelve

Entrega autenticación real con usuarios persistidos, passwords cifradas, JWT stateless y autorización backend por rol.

## 2. Alcance

Incluye:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- JWT con `sub`, `email`, `roles`, `iat`, `exp`
- Spring Security stateless
- BCrypt
- `@PreAuthorize`
- manejo consistente de `401` y `403`
- frontend con login, register, interceptor, guards y `AuthStore`

No incluye todavía CRUD de productos, inventario, órdenes, descuentos ni reportes.

## 3. Flujo backend

`AuthController -> RegisterUserUseCase/LoginUseCase -> Repository -> PostgreSQL`

En login:

1. Spring Security autentica email/password.
2. Se carga el usuario persistido.
3. `JwtService` emite el token firmado.
4. Se devuelve DTO seguro sin `password_hash`.

En request autenticada:

1. Angular envía `Authorization: Bearer <token>`.
2. Spring Security decodifica JWT.
3. Se derivan autoridades desde `roles`.
4. `@PreAuthorize` decide autorización.

## 4. Decisiones

- JWT evita estado de sesión en servidor.
- BCrypt resiste mejor filtraciones que contraseñas planas o hashing rápido.
- Guards Angular mejoran UX, pero la autorización real sigue en backend.
- DTOs separan API de entidades JPA y evitan exponer campos sensibles.

## 5. Pruebas

Backend:

- unit para hashing, JWT y `LoginUseCase`
- MockMvc para registro, login, duplicados, `401` y `403`

Frontend:

- store
- interceptor
- role guard

## 6. Cómo depurarlo

- `401`: revisar header `Authorization`, expiración y secret
- `403`: revisar `roles` del JWT y `@PreAuthorize`
- login fallido: revisar email, password cifrada y `AuthenticationManager`
- interceptor: revisar si el token existe en store/localStorage
