# Integración y entrega continua

Los workflows `backend-ci.yml` y `frontend-ci.yml` se ejecutan en push y pull request hacia `main`, con Java 21 y Node 22 explícitos. El backend publica JaCoCo y ejecuta `mvn clean verify`; el frontend ejecuta `npm ci`, lint, pruebas y build.

## Entrega continua

`release.yml` se activa en `main`, etiquetas `v*` y ejecución manual. Antes de publicar repite ambos gates para impedir que una imagen se entregue sin pruebas. Después construye backend y frontend para `linux/amd64` y `linux/arm64`, genera SBOM y provenance, firma una atestación vinculada al digest y publica en GHCR usando el `GITHUB_TOKEN` efímero.

Etiquetas:

- `latest`: último commit válido de `main`.
- `sha-<commit>`: despliegue inmutable y trazable.
- `v*`: versión de release creada desde una etiqueta Git.

`docker-compose.release.yml` consume esas imágenes y exige los secretos en el entorno del host. No contiene `build`, valores locales por defecto para secretos ni exposición directa de PostgreSQL/backend. Esto constituye Continuous Delivery hasta el registro. Continuous Deployment se añadirá cuando exista un destino real y sus GitHub Environment secrets; no se incluye un SSH o proveedor ficticio.

El job `publish` necesita `packages: write`, `attestations: write` e `id-token: write`; no requiere un PAT. En repositorios privados, el host debe autenticarse con `docker login ghcr.io` antes de hacer `pull`.
