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

## Decisiones relevantes

- PostgreSQL es la fuente persistente de verdad
- Flyway controla evolución de esquema
- Hibernate solo valida
- JWT y `@PreAuthorize` mantienen seguridad backend real
- DTOs aíslan API de entidades
- `JpaSpecificationExecutor` resuelve búsqueda dinámica sin memoria intermedia
