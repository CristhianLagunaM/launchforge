# PostgreSQL, Flyway y JPA en LaunchForge

## Ciclo real

Docker inicia PostgreSQL y crea una base vacía. `pg_isready` la marca healthy. Spring Boot abre el datasource, Flyway inspecciona `classpath:db/migration` y luego Hibernate ejecuta `validate`. En Fase 0 no hay entidades ni scripts SQL, por lo que una base vacía es el resultado correcto.

## Responsabilidades

- Docker entrega proceso, red, credenciales y volumen; no crea tablas de negocio.
- Flyway será la única fuente de verdad del esquema mediante V1–V8 según `migration-plan.md`.
- Hibernate mapea objetos y, con `ddl-auto=validate`, detecta divergencias; nunca crea ni altera el esquema.

Cuando existan migraciones, Flyway creará `flyway_schema_history` con versión, script, checksum, fecha y resultado. Una migración compartida no se edita; un cambio se expresa como V9 o la siguiente versión libre.

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## Constraints, índices, seed y tests

Se añadirán junto con cada feature, no en bootstrap. Constraints protegen invariantes en DB; índices deben responder a consultas reales; seed será determinista y separado conceptualmente de datos estructurales. Las pruebas críticas usarán Testcontainers PostgreSQL y las mismas migraciones; H2 no reproduciría SQL, locks y tipos PostgreSQL.

## Conectividad y depuración

Dentro de Compose la URL es `jdbc:postgresql://db:5432/launchforge`; desde un backend ejecutado en el host es `jdbc:postgresql://localhost:5432/launchforge`.

Secuencia de diagnóstico:

1. `docker compose ps`: confirmar `db` healthy.
2. `docker compose logs db`: revisar arranque, disco y credenciales.
3. `docker compose config`: comprobar variables resueltas.
4. `docker compose logs backend`: distinguir timeout, password, Flyway o validate.
5. `docker compose exec db pg_isready -U launchforge -d launchforge`.
6. Entrar con `psql` y consultar `flyway_schema_history` cuando ya existan migraciones.

Si Flyway falla, se corrige el SQL/estado local; no se deshabilita. Si Hibernate validate falla, se alinean entidad y migración; no se cambia a `update`.

