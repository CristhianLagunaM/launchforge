# Feature: bootstrap e infraestructura

## 1. Qué problema resuelve

Entrega una base reproducible para desarrollar LaunchForge sin introducir dominio antes de tiempo.

## 2. Flujo funcional

Compose inicia PostgreSQL; backend espera su salud, conecta, ejecuta Flyway y valida JPA; frontend espera backend y Nginx sirve Angular.

## 3. Archivos involucrados

Raíz: `docker-compose.yml`, `.env.example`, `.gitignore`, `Makefile`, `README.md`. Backend: `pom.xml`, `Dockerfile`, `application.yml`, clase principal, configuración de acceso y test. Frontend: configuración Angular/TypeScript, shell, test, Dockerfile y Nginx.

## 4. Flujo técnico paso a paso

`Browser → Nginx:80 → Angular`; futuras llamadas `/api → backend:8080 → JDBC → db:5432`. Actuator determina salud del backend.

## 5. Decisiones tomadas

Angular estable compatible con Node 24 en contenedor; Spring Boot 3.5/Java 21; PostgreSQL 17 explícito; monolito modular; Flyway habilitado sin migraciones de dominio. F09 alineó Angular y NgRx en la línea 21.

## 6. Por qué se hizo así

Se conserva un stack soportado y builds reproducibles. La carpeta vacía de migraciones prepara infraestructura sin violar el alcance Fase 0.

## 7. Alternativas comunes consideradas

Angular antiguo compatible con Node 20 local; `ddl-auto=update`; imagen única; `ng serve` en producción; `depends_on` corto sin healthcheck.

## 8. Por qué NO se eligieron esas alternativas

Angular antiguo ya no tiene soporte activo; Hibernate update oculta evolución; imágenes únicas pesan más; `ng serve` no es servidor productivo; proceso iniciado no equivale a servicio listo.

## 9. Principios SOLID aplicados

La separación de responsabilidades asigna entrega web a Nginx, API a Spring, esquema a Flyway y persistencia a PostgreSQL. No se crean abstracciones de dominio todavía.

## 10. Patrones utilizados

Contenedores multi-stage, configuración por entorno y health-based dependency ordering.

## 11. Malas prácticas que evitamos

No hay secretos reales versionados, tag `latest`, Maven/Node en runtime, tablas creadas manualmente, autenticación ficticia ni features adelantadas.

## 12. Riesgos y casos límite

Los puertos 80/5432/8080 pueden estar ocupados, por eso Compose permite override con `FRONTEND_HOST_PORT`, `BACKEND_HOST_PORT` y `DB_HOST_PORT`. `depends_on` no gestiona caídas posteriores. Node local 20 no ejecuta la versión Angular vigente.

## 13. Qué puede fallar

Descarga de imágenes/dependencias, credenciales inconsistentes, Docker Desktop detenido, DB sin espacio, healthcheck o ruta `dist` incorrectos.

## 14. Cómo depurarlo

Ejecutar `docker compose config`, `ps`, logs de DB/backend y el healthcheck manual. Aislar primero infraestructura, luego configuración, después aplicación.

## 15. Breakpoints recomendados

En Fase 0 solo `LaunchForgeApplication.main`; Actuator/Flyway son infraestructura de framework. Los breakpoints de casos de uso empiezan en fases posteriores.

## 16. Consultas SQL útiles para debugging

`SELECT current_database(), current_user;` y, cuando exista, `SELECT * FROM flyway_schema_history ORDER BY installed_rank;`.

## 17. Logs relevantes

Buscar pool Hikari, versión Flyway, validación Hibernate, puerto Tomcat y estado `Started LaunchForgeApplication`.

## 18. Pruebas existentes

JUnit confirma el entry point Spring Boot. Vitest crea el shell standalone. `mvn verify` genera JaCoCo; los tests de integración esperan schema real de fases futuras.

## 19. Cómo agregar una prueba nueva

Backend: clase `*Test` bajo el mismo package en `src/test/java`. Frontend: archivo `*.spec.ts` bajo `src`; ejecutar los comandos del README.

## 20. Cómo modificar esta feature en vivo

Cambiar una variable en `.env.example` y Compose de forma coordinada, validar con `docker compose config`, reconstruir solo el servicio y revisar su health.

## 21. Resumen

LaunchForge Fase 0 es un monorepo con Angular servido por Nginx, Spring Boot 3 sobre Java 21 y PostgreSQL 17. Compose usa red interna, volumen y healthchecks. Flyway está listo para versionar el schema y Hibernate solo valida. Builds y tests base son reproducibles; no existe aún ninguna feature de negocio.
