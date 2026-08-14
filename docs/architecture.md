# Architecture

LaunchForge mantiene un monorepo con tres piezas principales:

- `frontend`: Angular 22 standalone, Angular Material, stores con signals y routing
- `backend`: Spring Boot 3.5, JPA, Flyway, Security stateless y OpenAPI
- `db`: PostgreSQL 17 con esquema versionado por Flyway

## Capas backend

La organización actual sigue una separación pragmática:

- `api`: controllers y DTOs HTTP
- `application`: casos de uso, mappers y criterios de búsqueda
- `infrastructure`: repositorios JPA y acceso a persistencia
- `persistence.model`: entidades JPA
- `shared`: seguridad, excepciones y clases base

## Flujo principal de Fase 3

Público:

`Browser -> Angular catalog page -> GET /api/v1/products -> Specification -> PostgreSQL`

ADMIN:

`Angular admin page -> JWT interceptor -> ProductController -> ProductCatalogService -> Repository -> PostgreSQL`

## Flujo principal de Fase 4

ADMIN inventory:

`Angular admin inventory page -> JWT interceptor -> InventoryController -> InventoryManagementService -> InventoryRepository -> PostgreSQL`

Concurrencia:

`TX A + TX B -> leen inventory.version = N -> una confirma -> la otra falla por optimistic locking -> API 409`

## Flujo principal de Fase 5

Checkout:

`Angular cart/checkout -> OrdersStore -> OrdersApiService -> OrderController -> CreateOrderUseCase -> TransactionalOrderCreator -> InventoryManagementService + OrderRepository -> PostgreSQL`

Consulta:

`Angular orders page -> OrdersApiService -> OrderController -> OrderQueryService -> OrderRepository -> PostgreSQL`

Cancelación:

`Angular order detail -> OrdersApiService -> OrderController -> CancelOrderUseCase -> InventoryManagementService.restoreCapacity -> PostgreSQL`

## Decisiones relevantes

- PostgreSQL es la fuente persistente de verdad
- Flyway controla evolución de esquema
- Hibernate solo valida
- JWT y `@PreAuthorize` mantienen seguridad backend real
- DTOs aíslan API de entidades
- `JpaSpecificationExecutor` resuelve búsqueda dinámica sin memoria intermedia
- `Inventory` centraliza invariantes de capacidad
- `@Version` protege capacidad contra race conditions sin locks en memoria
- la creación de órdenes usa una transacción corta y explícita
- la idempotencia se apoya en un header HTTP y una restricción única en PostgreSQL
- `order_items` conserva snapshots para proteger histórico frente a cambios del catálogo
