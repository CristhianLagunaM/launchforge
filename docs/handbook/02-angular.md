# Angular en LaunchForge

## Fase 2

Angular 22 implementa autenticación con formularios reactivos, interceptor HTTP y guards funcionales.

## Piezas principales

- `AuthStore` con NgRx Signal Store
- `AuthApiService`
- `authInterceptor`
- `authGuard`
- `roleGuard`
- pantallas `/login` y `/register`
- layout autenticado `/app`

## Flujo

1. Usuario envía formulario.
2. Componente llama método del store.
3. Store usa `AuthApiService`.
4. Backend responde JWT + usuario seguro.
5. Store persiste sesión en `localStorage`.
6. Interceptor agrega bearer token a requests futuras.

## Regla de seguridad

Los guards Angular solo controlan navegación. No sustituyen la autorización backend.

## Qué revisar si falla

- token persistido
- interceptor registrado en `provideHttpClient`
- rutas protegidas y `route.data.roles`
- respuestas `401/403`
