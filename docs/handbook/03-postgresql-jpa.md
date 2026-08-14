# PostgreSQL, Flyway y JPA en LaunchForge

## Ciclo real

Docker inicia PostgreSQL y crea una base vacía. `pg_isready` la marca healthy. Spring Boot abre el datasource, Flyway inspecciona `classpath:db/migration`, aplica `V1` a `V9` y luego Hibernate ejecuta `validate`.

## Responsabilidades

- Docker entrega proceso, red, credenciales y volumen; no crea tablas de negocio.
- Flyway es la única fuente de verdad del esquema mediante `V1–V9` según `migration-plan.md`.
- Hibernate mapea objetos y, con `ddl-auto=validate`, detecta divergencias; nunca crea ni altera el esquema.

`flyway_schema_history` registra versión, descripción, script, checksum, fecha, usuario instalador y resultado. Una migración compartida no se edita; un cambio se expresa como `V9__...` o la siguiente versión libre.

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## Constraints, índices, seed y tests

Fase 1 ya instala constraints, índices y seed determinista:

- constraints como última línea de defensa del dominio;
- índices alineados con consultas reales de catálogo, órdenes y auditoría;
- seed demo reproducible con BCrypt y fechas UTC;
- tests con Testcontainers usando PostgreSQL real y las mismas migraciones de producción.

No usar H2: no reproduce SQL PostgreSQL, `JSONB`, índices parciales ni comportamiento de locking.

## Mapeos acordados

- Dinero: `NUMERIC(19,2)` ↔ `BigDecimal`
- Timestamps: `TIMESTAMPTZ` ↔ `Instant`
- Estado de orden: `EnumType.STRING`
- Metadata de auditoría: `JSONB` ↔ `Map<String, Object>`
- Lock de inventario: columna `version` ↔ `@Version`

## Orden de migraciones

1. `V1__create_identity.sql`
2. `V2__create_catalog.sql`
3. `V3__create_inventory.sql`
4. `V4__create_orders.sql`
5. `V5__create_discounts.sql`
6. `V6__create_audit.sql`
7. `V7__create_indexes.sql`
8. `V8__seed_demo_data.sql`
9. `V9__create_product_search_indexes.sql`

## Conectividad y depuración

Dentro de Compose la URL es `jdbc:postgresql://db:5432/launchforge`; desde un backend ejecutado en el host es `jdbc:postgresql://localhost:5432/launchforge`.

Secuencia de diagnóstico:

1. `docker compose ps`: confirmar `db` healthy.
2. `docker compose logs db`: revisar arranque, disco y credenciales.
3. `docker compose config`: comprobar variables resueltas.
4. `docker compose logs backend`: distinguir timeout, password, Flyway o validate.
5. `docker compose exec db pg_isready -U launchforge -d launchforge`.
6. Entrar con `psql` y consultar `flyway_schema_history`.
7. Comparar el nombre exacto de columna/constraint entre SQL y entidades JPA.

Si Flyway falla, se corrige el SQL o se recrea el entorno local; no se deshabilita. Si Hibernate `validate` falla, se alinean entidad y migración; no se cambia a `update`.
