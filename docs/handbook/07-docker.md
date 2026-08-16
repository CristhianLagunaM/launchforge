# Docker en LaunchForge

## Compose

```text
db -> backend -> frontend
```

`docker compose up --build`:

1. crea red/volumen;
2. inicia PostgreSQL;
3. espera `pg_isready`;
4. inicia backend;
5. espera Actuator;
6. inicia frontend.

## Puertos

Con `.env.example`:

```text
Frontend   8088 -> 80
Backend    8080 -> 8080
PostgreSQL 55432 -> 5432
```

DNS interno:

```text
db:5432
backend:8080
```

`localhost` dentro del contenedor backend no apunta a PostgreSQL.

## Healthchecks

PostgreSQL:

```text
pg_isready
```

Backend:

```text
/actuator/health
```

`depends_on.condition: service_healthy` controla el arranque inicial, no la disponibilidad permanente.

## Multi-stage

Backend:

```text
Maven/JDK -> jar -> JRE
```

Frontend:

```text
Node -> ng build -> Nginx
```

Producción no usa `ng serve`.

## Comandos

```bash
docker compose config
docker compose ps
docker compose logs backend
docker compose logs db
docker compose exec db psql -U launchforge -d launchforge
```

Reset:

```bash
docker compose down -v
docker compose up --build
```

## Secretos

`.env.example` contiene valores locales.

`.env` real no debe versionarse.

En entornos compartidos se deben reemplazar password de DB y JWT secret.
