# F09 — Frontend completeness e integración

## Alcance

La interfaz integra las capacidades del backend sin reproducir reglas de negocio sensibles en el navegador.

## Arquitectura

- `core/`: auth, guards, interceptor, API clients, modelos y stores compartidos;
- `shared/`: piezas reutilizables;
- `features/`: páginas standalone lazy;
- estado compartido con NgRx Signals;
- Reactive Forms para estado temporal.

## Routing

Público:

- `/login`;
- `/register`;
- `/catalog`;
- `/products/:id`.

Autenticado:

- carrito;
- checkout;
- órdenes.

Administración:

- `/admin/**` con `roleGuard` `ADMIN`.

Spring Security sigue siendo la frontera real.

## HTTP

Los clientes usan `/api/v1`.

En Compose:

```text
Browser -> Nginx -> /api -> backend:8080
```

## Checkout

- bloquea doble submit;
- conserva `Idempotency-Key` mientras la intención no cambia;
- cambiar carrito invalida la llave;
- carrito se limpia únicamente después de crear la orden;
- captura requerimientos del proyecto requeridos por backend.

## Errores

Problem Details tiene prioridad.

Se diferencian:

- 401;
- 403;
- validación;
- 409;
- red.

Un conflicto de inventario permite recargar la versión actual.

## Diseño

La identidad visual mantiene:

- modo oscuro;
- superficies de grafito;
- acentos cyan/violeta;
- densidad mayor en administración;
- Angular Material;
- Material Icons autoalojados;
- layout responsive;
- `prefers-reduced-motion`.

La presentación no modifica contratos, seguridad ni reglas.

## Reportes

El frontend presenta los datos ya calculados por backend.

No recalcula métricas financieras ni rankings.

## Descuentos

La pantalla administrativa traduce códigos a descripciones funcionales. Fechas/horas locales se convierten a UTC antes de enviar.

## Verificación

```bash
cd frontend
npm ci
npm run lint
npm run test -- --watch=false
npm run build
```

Después validar:

```bash
docker compose up --build
```
