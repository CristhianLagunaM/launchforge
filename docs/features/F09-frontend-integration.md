# F09 — Frontend completeness e integración

## Alcance

Esta fase cierra la interfaz de las capacidades existentes. Descuentos, disponibilidad, autorización e idempotencia efectiva siguen siendo responsabilidad del backend.

## Arquitectura

- `core/`: sesión, guards, interceptor, clientes HTTP, modelos y stores compartidos.
- `shared/`: piezas visuales reutilizables cuando existe reutilización real.
- `features/`: páginas standalone cargadas de forma diferida.
- Estado compartido: Auth, catálogo, carrito, órdenes y stores administrativos existentes.
- Estado temporal: Reactive Forms locales a cada pantalla.

## Routing y seguridad

Las rutas públicas son `/login`, `/register`, `/catalog` y `/products/:id`. Carrito, checkout y órdenes requieren `authGuard`. `/admin/**` requiere también `roleGuard` con `ADMIN`. Los guards son UX; Spring Security continúa siendo la frontera de autorización.

El catálogo y el detalle son públicos, pero comprar no lo es: las acciones anónimas llevan al login y `CartStore` rechaza `addItem` sin una sesión válida. El login conserva un `redirectUrl` local seguro para devolver al usuario al punto de compra.

## HTTP y despliegue

Los clientes usan `/api/v1`. En producción Nginx sirve la SPA y reenvía `/api/` a `backend:8080`. El interceptor añade el JWT y centraliza 401/403 sin mostrar el token.

## Checkout

El carrito valida cantidades. Checkout bloquea doble submit y conserva `Idempotency-Key` al reintentar el mismo contenido. Cambiar el carrito invalida la key porque representa una intención nueva. El carrito solo se limpia después de recibir la orden.

## Errores y accesibilidad

Problem Details tiene prioridad, con mensajes de respaldo para red, validación, 401, 403 y 409. Un conflicto optimista de inventario recarga la versión vigente. Los controles tienen labels o nombres accesibles y el layout se adapta a desktop, tablet y móvil.

Los conflictos por capacidad insuficiente se presentan como mensajes accionables en español. El backend extiende Problem Details con producto, SKU, cantidad solicitada y cantidad disponible; la interfaz resalta la línea afectada sin exponer UUID. Se conserva el `409` para permitir que el usuario corrija el carrito y reintente con la misma intención de compra.

Al volver al carrito y entrar nuevamente al checkout se limpia el feedback del intento anterior. La nueva cantidad genera una intención de carrito vigente y no conserva resaltados ni cifras obsoletas, mientras que la idempotencia continúa protegiendo cada intento real de creación.

La dirección visual es una interfaz oscura propia con rejilla sutil, vidrio y acentos cian/violeta. Los visuales usan Material Icons 1.13.14 autoalojados para no depender de una CDN. El hero utiliza `frontend/public/images/launchforge-hero-v1.png`, una imagen original generada para el proyecto sin texto, marcas, personajes conocidos ni recursos copiados de plantillas externas.

El catálogo mantiene tarjetas alineadas mediante una retícula adaptable y asigna un icono estable a cada categoría. Catálogo y detalle muestran una confirmación accesible al agregar un producto al carrito. Inventario reserva altura real para el contenido de cada fila y reportes presenta los rankings del backend como indicadores y barras comparativas, sin recalcular ni reordenar métricas de negocio en el navegador.

Login, registro y el inicio autenticado comunican beneficios para el usuario en lugar de revelar detalles internos de JWT, hashing o vencimiento de sesión. Las transiciones de acceso respetan `prefers-reduced-motion`.

La interfaz utiliza español para acciones, ayuda, fechas y paginación. Los identificadores técnicos que forman parte natural del contrato o del dominio de desarrollo —como SKU, slug, enums y nombres comerciales registrados en inglés— conservan su forma original.

## Refinamiento visual

La segunda pasada visual conserva el modo oscuro, el hero tecnológico, el cohete, la marca LaunchForge y el cyan oficial. Se incorporó un sistema común de tokens para fondo, superficies, bordes y tres niveles de texto; el cyan queda reservado para identidad, selección y datos relevantes en vez de funcionar como color universal de interacción.

El hero `frontend/public/images/launchforge-hero-v2.png` sustituye visualmente al recurso anterior sin eliminarlo del repositorio. Su dirección editorial muestra infraestructura computacional física, cableado y una zona operativa realista en grafito, acero, cyan escaso y una luz ámbar contenida. Se evitan deliberadamente los patrones de imagen generativa más repetidos: personaje con visor, hologramas, ciudad cyberpunk, gradientes magenta, saturación neón, cerebro neuronal literal y composición simétrica de stock.

Las vistas operativas aumentan su densidad sin alterar su composición ni comportamiento. Usuarios separa claramente nombre y correo; órdenes reduce la altura de sus filas; inventario utiliza un borde seleccionado sin resplandor; descuentos distingue la regla activa con una señal mínima; auditoría mantiene identificadores técnicos en monoespaciada y filas de bajo contraste. El catálogo conserva mayor espacio, arte futurista y hero, pero usa movimientos de uno o dos píxeles, sombras contenidas y metadata secundaria para evitar competencia visual.

Inputs, navegación y estados de foco comparten superficies oscuras, radios técnicos y transiciones de 150 ms. El cambio es exclusivamente de presentación: no modifica rutas, contratos HTTP, stores, seguridad, validaciones ni reglas de negocio.

La dirección antiplantilla se apoya en fichas técnicas, líneas estructurales, materiales fotográficos, metadata monoespaciada y contraste editorial. Las acciones principales usan un tono claro y las secundarias se mantienen neutrales; el cyan queda reservado para el cohete, pequeños indicadores, selección y cifras operativas. También se corrigió la convivencia entre controles nativos y `mat-form-field` para impedir el efecto visual de un segundo input incrustado dentro del campo Material.

Login y registro utilizan explícitamente la variante `outline` de Angular Material. El wrapper es la única superficie visible y el autocompletado de navegadores WebKit conserva el mismo fondo, texto y caret, evitando una segunda caja superpuesta. La portada autenticada abandonó el degradado azul-violeta residual y comparte ahora superficies de grafito, retícula técnica, radios y densidad con administración, catálogo y reportes.

Reportes utiliza un ámbar desaturado (`#A87932` a `#D6B46A`) como segunda codificación visual. Este acento cálido diferencia clientes frecuentes de las métricas cyan sin competir con la marca ni confundirse con el verde reservado para estados correctos.

El tablero de reportes organiza la información en tres niveles: KPI financieros, tendencia y estado operativo, y rankings detallados. La evolución mensual se representa sin dependencias gráficas adicionales; estados y alertas usan códigos cromáticos semánticos. El hero del catálogo reemplaza la enumeración técnica de filtros por una invitación centrada en el proyecto del cliente; los filtros permanecen disponibles en su sección específica.

La administración de descuentos traduce los códigos de regla a nombres y descripciones funcionales. Las ventanas temporales se editan mediante calendario y hora local; antes de enviar la actualización se convierten a un instante UTC para conservar el contrato y evitar ambigüedades en el backend.

## Verificación

```bash
cd frontend
npm ci
npm run lint
npm run test -- --watch=false
npm run build
```

Finalmente se valida `docker compose up --build` y los flujos de cliente y administrador.
