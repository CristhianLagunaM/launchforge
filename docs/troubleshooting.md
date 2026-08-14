# Troubleshooting

## 1. `401 Unauthorized`

- verificar que el request admin incluya `Authorization: Bearer <jwt>`
- revisar expiración del token
- confirmar que el interceptor frontend siga registrado

## 2. `403 Forbidden`

- revisar roles dentro del JWT
- confirmar `ROLE_ADMIN` en backend
- probar el mismo endpoint con un token del admin demo

## 3. `404 Product not found`

- confirmar UUID correcto
- si el request es público, verificar que el producto siga activo
- revisar si la UI usa un id viejo después de editar/eliminar

## 4. `409 Conflict`

- consultar productos por `sku` o `slug`
- revisar si la edición intenta reutilizar un valor ya existente

## 5. Búsqueda vacía o inesperada

- validar `minPrice <= maxPrice`
- revisar `category` por nombre o slug
- recordar que catálogo público fuerza `active=true`
- revisar `available=true` contra `inventory.available_quantity`

## 6. Docker Compose no levanta

- `docker compose ps`
- `docker compose logs backend`
- `docker compose logs frontend`
- `docker compose logs db`

Si cambiaste migraciones o seed, reinicio limpio local:

```bash
docker compose down -v
docker compose up --build
```
