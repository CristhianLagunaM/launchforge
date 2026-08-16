# Feature: catálogo de productos

## Alcance

Catálogo público y administración segura de productos.

## Público

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `GET /api/v1/categories`

Filtros:

- `name`;
- `sku`;
- `category`;
- `minPrice`;
- `maxPrice`;
- `active`;
- `available`;
- paginación;
- sorting.

## ADMIN

- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `PATCH /api/v1/products/{id}/status`
- `DELETE /api/v1/products/{id}`

## Flujo

```text
ProductController
 -> ProductCatalogService
 -> ProductSpecifications
 -> ProductRepository
 -> PostgreSQL
```

Las búsquedas no cargan todo el catálogo para filtrar en memoria.

## DTO

Controllers retornan DTOs y no entidades JPA.

## Inventario inicial

Al crear producto se conserva la relación esperada con inventario; la capacidad se administra desde el módulo de inventario.

## Eliminación

- sin historial: puede eliminarse físicamente;
- con historial comercial: se preserva el producto y se desactiva.

## Índices

Se utilizan índices para:

- activo;
- categoría;
- nombre;
- precio;
- disponibilidad.

## Errores

- `400`: filtros/sort/validación;
- `401`: sin autenticación cuando aplica;
- `403`: sin rol;
- `404`: producto/categoría;
- `409`: SKU/slug.

## Prueba rápida

```bash
curl "http://localhost:8080/api/v1/products?name=landing&page=0&size=5&sort=name,asc"
```
