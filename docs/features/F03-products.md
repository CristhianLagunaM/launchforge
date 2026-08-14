# Feature: catálogo de productos

## 1. Qué problema resuelve

Entrega el primer módulo de negocio completo sobre el modelo persistente existente: catálogo público consultable y administración segura de productos.

## 2. Flujo funcional

Público:

- consulta `GET /api/v1/products`
- filtra por nombre, SKU, categoría, precio y disponibilidad
- navega por páginas y ordena resultados
- consulta `GET /api/v1/products/{id}` para detalle

ADMIN:

- crea producto
- actualiza producto
- activa/desactiva producto
- elimina físicamente solo si nunca tuvo historial comercial
- si el producto ya fue usado, `DELETE` aplica desactivación lógica con `active=false`

## 3. Flujo técnico

`ProductController -> ProductCatalogService -> ProductRepository/JpaSpecificationExecutor -> PostgreSQL`

Lecturas:

1. controller recibe query params;
2. arma `ProductSearchCriteria` y `Pageable`;
3. `ProductSpecifications` construye predicates dinámicos;
4. JPA ejecuta filtros y paginación en DB;
5. `ProductMapper` transforma entidades a DTOs.

Escrituras:

1. controller valida DTO;
2. `@PreAuthorize` exige `ADMIN`;
3. service valida SKU, slug y categoría;
4. persiste `products`;
5. en altas crea inventario base `0/0` para mantener la relación.

## 4. Endpoints

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `PATCH /api/v1/products/{id}/status`
- `DELETE /api/v1/products/{id}`
- `GET /api/v1/categories`

## 5. Clases principales

- `ProductController`
- `CategoryController`
- `ProductCatalogService`
- `ProductMapper`
- `ProductSpecifications`
- `ProductRepository`
- `CategoryRepository`
- `InventoryRepository`
- `OrderItemRepository`

## 6. Búsqueda dinámica

Se implementa con `JpaSpecificationExecutor` para evitar:

- `findAll()` completo;
- filtrado en memoria;
- un endpoint por filtro;
- `if/else` gigante con consultas duplicadas.

Filtros soportados:

- `name`
- `sku`
- `category`
- `minPrice`
- `maxPrice`
- `active`
- `available`

La paginación y el sorting se delegan al motor de base de datos mediante `PageRequest`.

## 7. DTO vs Entity

Los controllers no retornan entidades JPA.

Razones:

- no exponer estructura interna de persistencia;
- evitar acoplar API y modelo relacional;
- controlar campos visibles y estabilidad del contrato;
- facilitar evolución de mappers y validaciones.

## 8. Soft deletion

La regla del modelo se respeta así:

- producto sin historial comercial: puede borrarse físicamente;
- producto ya referenciado por `order_items`: `DELETE` lo desactiva.

Esto preserva consistencia histórica y evita romper órdenes antiguas.

## 9. Índices relevantes

El catálogo ya tenía:

- `products(active)`
- `products(category_id)`
- `products(name)`

Fase 3 agrega:

- `products(price)`
- `inventory(available_quantity)`

Estos índices apoyan filtros por precio y disponibilidad.

## 10. Errores y seguridad

- `404`: producto o categoría inexistente
- `409`: SKU o slug duplicados
- `400`: validaciones o sort/rango inválidos
- `401`: operación protegida sin JWT
- `403`: JWT válido sin rol `ADMIN`

Lecturas de catálogo son públicas, pero productos inactivos solo se exponen a `ADMIN`.

## 11. Cómo probar la feature

Swagger:

- abrir `/swagger-ui/index.html`
- probar `GET /api/v1/products`
- autenticar como admin y probar `POST`, `PUT`, `PATCH`, `DELETE`

Ejemplo `curl` público:

```bash
curl "http://localhost:8080/api/v1/products?name=landing&category=web&page=0&size=5&sort=name,asc"
```

Ejemplo `curl` admin:

```bash
curl -X POST "http://localhost:8080/api/v1/products" \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "sku":"LF-NEW-001",
    "name":"New Product",
    "slug":"new-product",
    "description":"New package",
    "categoryId":1,
    "price":1500.00
  }'
```

## 12. Troubleshooting técnico

- `409` al crear: revisar SKU y slug existentes
- `404` al guardar: revisar `categoryId` o `productId`
- `400` en búsqueda: revisar `minPrice/maxPrice` y `sort`
- resultados vacíos: verificar `active=true` implícito para usuarios anónimos
- `DELETE` no borra físicamente: revisar si el producto ya existe en `order_items`
