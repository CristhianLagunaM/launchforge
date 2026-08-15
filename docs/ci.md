# Integración continua

Los workflows `backend-ci.yml` y `frontend-ci.yml` se ejecutan en push y pull request hacia `main`, con Java 21 y Node 22 explícitos. El backend publica JaCoCo y ejecuta `mvn clean verify`; el frontend ejecuta `npm ci`, lint, pruebas y build. No se simula despliegue productivo: esta fase entrega CI reproducible.
