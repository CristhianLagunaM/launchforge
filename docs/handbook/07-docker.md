# Docker en LaunchForge

## Ciclo de Compose

`docker compose up --build` construye backend y frontend, crea la red y el volumen, y arranca `db → backend → frontend`. `down` elimina contenedores/red pero conserva `postgres_data`; `down -v` elimina también el volumen y es un reset irreversible de datos locales.

## Red y puertos

Compose crea la red interna `launchforge`. DNS interno resuelve nombres de servicio: backend conecta a `db:5432` y Nginx reenvía `/api` a `backend:8080`. `localhost` dentro de backend sería el propio contenedor backend, no PostgreSQL. Los puertos publicados son 80, 8080 y 5432.

## Healthchecks y orden

PostgreSQL ejecuta `pg_isready` cada 5 segundos. Esto verifica que acepta conexiones; un proceso iniciado no implica un servicio listo. `depends_on.condition: service_healthy` retrasa backend hasta que DB esté healthy. Backend se comprueba con `wget --spider http://localhost:8080/actuator/health`, disponible en Alpine. Frontend espera backend healthy.

`depends_on` controla el orden inicial, no reinicia automáticamente un consumidor si su dependencia falla después. La aplicación aún debe tolerar fallos transitorios.

## Imágenes multi-stage

Backend compila con Maven/JDK 21 y copia solo el jar a una imagen JRE 21 ejecutada por usuario no root. Frontend compila con Node 24.15 y copia `dist/.../browser` a Nginx; producción nunca usa `ng serve`. Los `.dockerignore` excluyen dependencias y artefactos locales.

## Diagnóstico

```bash
docker compose ps
docker compose logs backend
docker compose logs db
docker compose exec db psql -U launchforge -d launchforge
docker inspect launchforge-backend-1
```

Si health falla, probar el comando exacto dentro del contenedor, revisar variables con `docker compose config` y comparar logs de DB/backend. Para reinicio limpio: `docker compose down -v` y `docker compose up --build`.

## Volúmenes y secretos

`postgres_data` conserva datos entre `down/up`. `.env` no se versiona; `.env.example` solo contiene valores locales inseguros. Compose sustituye variables del host y pasa al backend la URL interna.

