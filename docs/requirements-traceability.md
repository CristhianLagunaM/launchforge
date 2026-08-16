# Matriz de trazabilidad

| Requisito | Implementación | Endpoint/módulo | Evidencia | Estado |
|---|---|---|---|---|
| Login y registro | JWT stateless + BCrypt | `/api/v1/auth` / `auth` | `AuthControllerMockMvcTest`, `LoginUseCaseTest` | COMPLETED |
| Catálogo y búsqueda | Specifications, paginación y sorting | `/api/v1/products` / `catalog` | `ProductCatalogIntegrationTest`, `ProductControllerMockMvcTest` | COMPLETED |
| CRUD de productos | Operaciones protegidas ADMIN | `/api/v1/products` | `ProductControllerMockMvcTest` | COMPLETED |
| Inventario | Capacidad disponible/reservada, optimistic locking y operaciones de ajuste | `/api/v1/inventory` / `InventoryManagementService` | `InventoryConcurrencyIntegrationTest`, `InventoryManagementServiceTest`, V12 | COMPLETED |
| Órdenes | Estado `CREATED`, reserva, confirmación ADMIN, snapshot, idempotencia y cancelación | `/api/v1/orders`, `/api/v1/orders/admin`, `PATCH /{id}/confirm` | tests de órdenes y Testcontainers | COMPLETED |
| Descuentos | Time range, random order y frequent customer | `/api/v1/discounts` / `discounts` | `DiscountEngineTest`, `DiscountIntegrationTest` | COMPLETED |
| Reportes | Agregaciones SQL en PostgreSQL | `/api/v1/reports` | `ReportRepositoryIntegrationTest`, `ReportControllerMockMvcTest` | COMPLETED |
| Auditoría | AOP transaccional para creación, confirmación y cancelación | `/api/v1/audit` | `AuditIntegrationTest`, `AuditControllerMockMvcTest` | COMPLETED |
| Gestión administrativa de usuarios | No existe controller CRUD de usuarios en la implementación actual | — | Revisión de endpoints | PARTIAL |
| Migraciones y seed | Flyway V1–V12, Hibernate validate | `db/migration` | `PersistenceIntegrationTest`, compose | COMPLETED |
| Docker | db/backend/frontend, healthchecks y depends_on | `docker-compose.yml` | `docker compose config` | COMPLETED |
| CI y análisis estático | GitHub Actions, JaCoCo, Spotless, PMD, ESLint | `.github/workflows` | workflows versionados | COMPLETED |
