# F11 — Gestión administrativa de usuarios y descuentos

## Usuarios

ADMIN puede consultar `GET /api/v1/admin/users` y actualizar estado/rol con `PATCH /api/v1/admin/users/{id}`. La operación no recibe ni modifica `passwordHash`, nombre o correo; únicamente activa/bloquea y asigna `ADMIN` o `CUSTOMER`. La autorización se aplica con `@PreAuthorize("hasRole('ADMIN')")`.

Frontend: `/admin/users`.

## Descuentos en órdenes

Los descuentos se calculan únicamente al confirmar una orden, no al agregar productos al carrito. La respuesta de la orden contiene `discountTotal`, `total` y `discounts` con código, porcentaje, base, importe y motivo.

Para probarlos:

1. Iniciar sesión como `frequent@launchforge.dev` o configurar una regla desde `/admin/discounts`.
2. Mantener `Habilitado`, un rango UTC que incluya la fecha/hora actual y porcentajes válidos.
3. Crear una orden nueva; las órdenes históricas no se recalculan.
4. Revisar el detalle de la orden y `order_discounts` en PostgreSQL.

El motor aplica las estrategias en orden `TIME_RANGE`, `RANDOM_ORDER`, `FREQUENT_CUSTOMER`, usando el total vigente como base de cada descuento sucesivo. `RANDOM_ORDER` además requiere que el proveedor aleatorio resulte ganador.
