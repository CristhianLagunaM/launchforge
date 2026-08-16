# Checklist de entrega

## Código y arquitectura

- [x] Repositorio versionado.
- [x] Backend Spring Boot / Java 21.
- [x] Frontend Angular 21.
- [x] PostgreSQL 17.
- [x] Monolito modular documentado.
- [x] Sin secretos reales versionados.

## Funcionalidad requerida

- [x] Login.
- [x] Registro.
- [x] Gestión administrativa de usuarios.
- [x] CRUD y búsqueda de productos.
- [x] Inventario.
- [x] Órdenes.
- [x] Descuento por rango temporal.
- [x] Descuento aleatorio.
- [x] Descuento de cliente frecuente.
- [x] Reporte de productos activos.
- [x] Top 5 productos.
- [x] Top 5 clientes.
- [x] Auditoría administrativa.

## Persistencia

- [x] Flyway como fuente del esquema.
- [x] Migraciones `V1` a `V16`.
- [x] Hibernate `ddl-auto=validate`.
- [x] Instalación reproducible sobre PostgreSQL vacío.
- [x] Bootstrap del primer `ADMIN` documentado.
- [x] No se requieren usuarios demo.

## Calidad

- [x] Tests backend.
- [x] Tests frontend.
- [x] Testcontainers PostgreSQL.
- [x] JaCoCo.
- [x] Spotless / PMD.
- [x] ESLint.
- [x] Backend CI verde en `main`.
- [x] Frontend CI verde en `main`.

## Contenedores y entrega

- [x] Docker Compose con DB/backend/frontend.
- [x] Healthchecks.
- [x] Continuous Delivery a GHCR.
- [x] Imágenes multi-arquitectura.
- [x] SBOM y provenance.
- [x] Etiquetas por commit.
- [x] Compose de release.
- [x] Continuous Deployment identificado como fuera de alcance hasta definir proveedor.

## Documentación

- [x] README de ejecución.
- [x] Arquitectura.
- [x] Modelo de dominio.
- [x] API.
- [x] Seguridad.
- [x] Testing.
- [x] Calidad.
- [x] CI/CD.
- [x] Migraciones.
- [x] Features.
- [x] ADR.
- [x] Troubleshooting.
- [x] Matriz de trazabilidad.
- [x] Guion de demo.

## Validación final antes de enviar

- [ ] Ejecutar `docker compose down -v`.
- [ ] Ejecutar `docker compose up --build`.
- [ ] Confirmar `db` healthy.
- [ ] Confirmar `backend` healthy.
- [ ] Confirmar frontend accesible.
- [ ] Registrar usuario desde cero.
- [ ] Validar bootstrap inicial `ADMIN`.
- [ ] Ejecutar flujo funcional completo.
- [ ] Ejecutar `mvn clean verify`.
- [ ] Ejecutar lint/tests/build frontend.
- [ ] Confirmar Actions verdes en el commit final.
- [ ] Grabar y adjuntar el video de funcionamiento.

> El video es un entregable externo y se marca únicamente cuando haya sido grabado sobre la versión final.
