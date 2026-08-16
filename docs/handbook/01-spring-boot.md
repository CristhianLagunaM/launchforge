# Spring Boot en LaunchForge

## Rol

Spring Boot expone la API, ejecuta casos de uso, aplica seguridad y coordina persistencia.

## Flujo de login

1. `AuthController` recibe DTO.
2. `LoginUseCase` delega autenticación.
3. `DatabaseUserDetailsService` carga usuario.
4. BCrypt valida password.
5. `JwtService` firma el token.
6. se devuelve DTO seguro.

## Flujo general

```text
Controller
 -> Application / Use Case
 -> Repository
 -> PostgreSQL
```

## Puntos importantes

- controllers no retornan entidades JPA;
- `password_hash` nunca sale de backend;
- Problem Details normaliza errores;
- `@EnableMethodSecurity` habilita `@PreAuthorize`;
- `@Transactional` define fronteras de negocio;
- auditoría AOP es transversal.

## Beans críticos de seguridad

- `PasswordEncoder`;
- `AuthenticationManager`;
- `JwtEncoder`;
- `JwtDecoder`;
- `SecurityFilterChain`.

## Diagnóstico

Revisar:

- `JWT_SECRET`;
- `JWT_EXPIRATION_SECONDS`;
- roles en DB;
- `UserDetailsService`;
- `JwtAuthenticationConverter`;
- `@PreAuthorize`;
- logs de Spring Security.
