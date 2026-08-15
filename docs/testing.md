# Testing

## Backend

Se cubren tres niveles:

- unit tests para lógica de catálogo
- integration tests con PostgreSQL/Testcontainers para búsqueda y paginación
- MockMvc para contrato HTTP, seguridad y validaciones
- unit tests para invariantes de inventario
- integración concurrente real para `inventory.version`
- unit tests para creación, idempotencia y cancelación de órdenes
- MockMvc para endpoints de órdenes y ownership
- unit tests del `DiscountEngine`
- integration tests de descuentos con PostgreSQL/Testcontainers
- MockMvc para configuración admin de descuentos
- integration tests PostgreSQL para las tres queries de reportes
- MockMvc para autorización y contrato de reportes

## Frontend

Se prueban:

- `CatalogStore`
- interceptor y guards de auth existentes
- formulario de producto
- `CartStore`
- `OrdersStore`
- `AdminDiscountStore`
- `ReportStore`

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

### Aislamiento de fixtures mutables

Las pruebas que modifican inventario u órdenes preparan un estado conocido y restauran los datos compartidos cuando corresponde:

- los tests HTTP eliminan únicamente las órdenes creadas con su prefijo reservado de idempotencia;
- la prueba concurrente restaura el inventario demo al finalizar;
- las entidades nuevas dejan UUID y versión sin asignar para que JPA las reconozca como nuevas;
- la versión esperada se consulta en PostgreSQL después de preparar el escenario concurrente.

Así, el resultado no depende del orden de ejecución de JUnit. Los reintentos idempotentes también se consultan y transforman a DTO dentro de una transacción de solo lectura, manteniendo disponibles las relaciones lazy durante el mapeo.

## Qué valida Fase 5

- creación de órdenes confirmadas
- consolidación de items repetidos
- snapshot de nombre, SKU y precio
- idempotencia por `Idempotency-Key`
- rechazo de producto inactivo
- rechazo por capacidad insuficiente
- ownership de lectura
- cancelación con restauración de capacidad

## Qué valida Fase 6

- cálculo acumulable sobre subtotal original con orden de trazabilidad `TIME_RANGE -> RANDOM_ORDER -> FREQUENT_CUSTOMER`
- query `COUNT` para cliente frecuente
- `RANDOM_ORDER` testeable sin aleatoriedad real
- persistencia de `order_discounts`
- conservación histórica del desglose aunque cambie la configuración
- edición admin de `discount_configuration`

## Validación manual recomendada para descuentos

1. autenticar `frequent@launchforge.dev`;
2. crear una orden dentro de un rango activo;
3. verificar el detalle y el arreglo `discounts`;
4. editar una regla desde `/app/admin/discounts`;
5. crear otra orden y comparar el nuevo cálculo;
6. ejecutar la consulta SQL sobre `order_discounts`;
7. cambiar la configuración y confirmar que una orden histórica no se alteró.

## Validación manual recomendada para órdenes

1. autenticar un `CUSTOMER`;
2. crear una orden con `Idempotency-Key`;
3. repetir el mismo `POST` con la misma llave;
4. listar órdenes del cliente;
5. consultar detalle;
6. cancelar una orden confirmada;
7. verificar en SQL que la capacidad fue restaurada.

## Qué valida Fase 7

- productos inactivos excluidos;
- suma de cantidades por producto en PostgreSQL;
- solo estados `CONFIRMED/COMPLETED`;
- `CANCELLED` excluidas;
- límite cinco con más de cinco candidatos;
- desempates deterministas;
- conteo de órdenes por cliente;
- `ADMIN 200`, `CUSTOMER 403`, anónimo `401`;
- estados loading/error y mapping del store frontend.

Los tests de repository reemplazan el seed dentro de una transacción y crean datos controlados; no dependen de los rankings demo.

## Qué valida Fase 8

- PRODUCT_UPDATED, INVENTORY_ADJUSTED y ORDER_CANCELLED generan eventos;
- actor y correlation ID corresponden al request;
- metadata contiene solo detalles permitidos;
- rollback no conserva una auditoría de éxito;
- consulta admin con filtros y paginación;
- ADMIN 200, CUSTOMER 403 y anónimo 401;
- carga paginada y error de AuditStore.
