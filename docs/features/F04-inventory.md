# Feature: inventory management y concurrencia

## 1. Qué representa inventario en LaunchForge

`inventory` no modela unidades físicas.

Modela capacidad operativa disponible para iniciar nuevos proyectos.

Ejemplos:

- Landing Page: 8 cupos
- E-commerce: 3 cupos
- MVP SaaS: 2 cupos

Esto evita mezclar disponibilidad comercial del catálogo con capacidad operativa del equipo.

## 2. Endpoints

Solo `ADMIN`:

- `GET /api/v1/inventory`
- `GET /api/v1/inventory/{productId}`
- `PATCH /api/v1/inventory/{productId}`

`PATCH` recibe:

- `operation`: `INCREASE`, `DECREASE`, `RESTORE`
- `quantity`
- `version`

## 3. Flujo técnico

`InventoryController -> InventoryManagementService -> InventoryRepository -> PostgreSQL`

Lectura:

1. controller recibe `page`, `size`, `sort` o `productId`;
2. service carga `inventory` con su `product`;
3. mapper transforma a `InventoryResponse`.

Ajuste:

1. controller valida DTO;
2. `@PreAuthorize` exige `ADMIN`;
3. service carga la fila de inventario;
4. valida versión del cliente;
5. aplica operación de dominio sobre `Inventory`;
6. `saveAndFlush` persiste;
7. un conflicto de versión se traduce a `409`.

## 4. Invariantes centralizadas

La lógica de capacidad vive en la entidad `Inventory`:

- `increase(int quantity)`
- `decrease(int quantity)`
- `restore(int quantity)`

No se dispersan operaciones tipo:

```java
setAvailableQuantity(getAvailableQuantity() - quantity)
```

por múltiples servicios.

## 5. Optimistic locking

La columna `inventory.version` ya existía en el modelo y se mapea con:

```java
@Version
private Long version;
```

Esto hace que Hibernate genere updates condicionados por versión. Si dos transacciones leen la misma versión y ambas intentan persistir cambios, solo una actualiza la fila; la otra falla.

## 6. Race condition y overselling

Sin control de concurrencia:

1. dos requests leen `available_quantity = 1`;
2. ambos descuentan 1;
3. ambas operaciones parecen válidas;
4. el resultado puede sobreasignar capacidad.

Con optimistic locking:

1. ambas transacciones leen versión `N`;
2. una persiste primero y deja versión `N + 1`;
3. la otra intenta actualizar con versión vieja;
4. Spring lanza conflicto optimista;
5. la API responde `409`.

## 7. Por qué no `synchronized`

`synchronized` o locks en memoria:

- solo protegen una instancia del backend;
- no coordinan múltiples pods o contenedores;
- no sobreviven a escalado horizontal.

El control real debe ocurrir sobre la fila persistida en PostgreSQL.

## 8. Cuándo usar pessimistic locking

Optimistic locking es apropiado cuando:

- la contención no es extrema;
- el costo de reintento es aceptable;
- se prioriza throughput.

Pessimistic locking sería razonable si:

- hubiera altísima contención sostenida;
- la operación crítica no tolerara reintento frecuente;
- el flujo futuro de órdenes demostrara demasiados conflictos.

En Fase 4 no se justifica introducirlo todavía.

## 9. Excepción y HTTP 409

Conflictos de negocio:

- inventario insuficiente
- versión obsoleta enviada por cliente
- conflicto optimista detectado por JPA/Spring

se responden como `409 Conflict` usando Problem Details.

## 10. Cómo probar

Swagger:

- autenticar como admin
- consultar `GET /api/v1/inventory`
- abrir una fila con `GET /api/v1/inventory/{productId}`
- aplicar `PATCH` con una operación y cantidad

Ejemplo:

```bash
curl -X PATCH "http://localhost:8080/api/v1/inventory/22222222-2222-2222-2222-222222222221" \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "operation":"DECREASE",
    "quantity":1,
    "version":0
  }'
```

## 11. Troubleshooting técnico

- `409 insufficient`: la cantidad solicitada supera la disponible
- `409 optimistic`: otro request ya modificó la misma fila
- `404`: el producto no tiene fila de inventario
- `400`: `quantity <= 0` o payload inválido
- datos inconsistentes en UI: recargar inventario para obtener la versión más reciente
