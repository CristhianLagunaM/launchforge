# Testing

## Backend

Se cubren tres niveles:

- unit tests para lógica de catálogo
- integration tests con PostgreSQL/Testcontainers para búsqueda y paginación
- MockMvc para contrato HTTP, seguridad y validaciones
- unit tests para invariantes de inventario
- integración concurrente real para `inventory.version`

## Frontend

Se prueban:

- `CatalogStore`
- interceptor y guards de auth existentes
- formulario de producto

## Comandos

Backend:

```bash
cd backend
mvn test
mvn clean package
```

Frontend:

```bash
cd frontend
npm test -- --watch=false
npm run lint
npm run build
```

Compose:

```bash
docker compose up --build
```

## Qué valida Fase 3

- CRUD de productos
- seguridad pública vs admin
- filtros de búsqueda en DB
- paginación
- sorting
- conflicto de SKU/slug
- DTOs y formularios

## Qué valida Fase 4

- endpoints admin de inventario
- operaciones `increase`, `decrease`, `restore`
- rechazo de inventario insuficiente
- rechazo de versión obsoleta
- optimistic locking real con dos transacciones concurrentes sobre stock `1`

## Prueba concurrente

La prueba de concurrencia no usa solo mocks.

Usa:

- PostgreSQL real vía Testcontainers
- dos transacciones reales
- misma fila `inventory`
- mismo `version`

Resultado esperado:

- una operación consume el único cupo
- la otra falla por optimistic locking
- el stock final queda en `0`
- nunca queda negativo
