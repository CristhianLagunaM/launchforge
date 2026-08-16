# F11 — Gestión administrativa de usuarios y descuentos

## Usuarios

Solo `ADMIN`:

```text
GET   /api/v1/admin/users
PATCH /api/v1/admin/users/{id}
```

La actualización modifica:

- `enabled`;
- rol.

No recibe ni expone:

- `password`;
- `passwordHash`.

La autorización está en:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Frontend:

```text
/admin/users
```

## Bootstrap del primer ADMIN

El primer administrador no puede depender de sí mismo para administrar roles.

Flujo inicial:

```mermaid
flowchart LR
    R[Registro normal] --> C[CUSTOMER]
    C --> DB[Asignación ADMIN en user_roles]
    DB --> L[Nuevo login]
    L --> A[/admin/users]
```

Los administradores posteriores se gestionan desde la aplicación.

## Descuentos

Los descuentos se calculan **al crear una orden** dentro de `TransactionalOrderCreator`.

No se espera a la confirmación.

```text
Create order
 -> reserve inventory
 -> DiscountEngine
 -> persist order + discounts
```

La respuesta contiene:

- `discountTotal`;
- `total`;
- `discounts`.

## Base de cálculo

Las reglas combinables utilizan el subtotal original como base.

```text
subtotal 100
10% -> 10
50% -> 50
5%  -> 5
total descuento = 65
```

No se encadena el segundo porcentaje sobre `90`.

## Configuración

`V15` deja las reglas deshabilitadas por defecto.

`ADMIN` puede:

- habilitarlas;
- definir porcentaje;
- definir rango;
- configurar cliente frecuente.

## Prueba

1. crear/promover un `ADMIN`;
2. configurar descuentos;
3. crear un usuario `CUSTOMER`;
4. generar las condiciones necesarias;
5. crear una nueva orden;
6. revisar `discounts` y `order_discounts`.

No se utilizan cuentas demo fijas.
