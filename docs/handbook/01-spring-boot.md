# Spring Boot en LaunchForge

## Fase 2

Spring Boot expone autenticación en `/api/v1/auth` y protege recursos con Spring Security stateless.

## Flujo de login

1. `AuthController` recibe DTO.
2. `LoginUseCase` delega en `AuthenticationManager`.
3. `DatabaseUserDetailsService` carga usuario desde PostgreSQL.
4. `BCryptPasswordEncoder` compara password.
5. `JwtService` firma el token.
6. Se responde `AuthResponse`.

## Puntos importantes

- No se retornan entidades JPA.
- No se expone `password_hash`.
- `ProblemDetail` normaliza errores.
- `@EnableMethodSecurity` habilita `@PreAuthorize`.

## Beans críticos

- `PasswordEncoder`
- `AuthenticationManager`
- `JwtEncoder`
- `JwtDecoder`
- `SecurityFilterChain`

## Qué revisar si falla

- propiedades `JWT_SECRET` y `JWT_EXPIRATION_SECONDS`
- roles seed en DB
- `UserDetailsService`
- `JwtAuthenticationConverter`
