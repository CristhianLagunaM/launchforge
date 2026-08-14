# Testing

## Backend

Se cubren tres niveles:

- unit tests para lógica de catálogo
- integration tests con PostgreSQL/Testcontainers para búsqueda y paginación
- MockMvc para contrato HTTP, seguridad y validaciones

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
