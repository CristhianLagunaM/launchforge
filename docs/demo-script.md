# Guion técnico de demo (8–12 minutos)

## Preparación

```bash
cp .env.example .env
docker compose up --build
```

URLs: frontend `http://localhost`, Swagger `http://localhost:8080/swagger-ui/index.html`, health `http://localhost:8080/actuator/health`.

## Recorrido

1. Presentar el problema, módulos y diagrama de arquitectura (1:30).
2. Mostrar login ADMIN/CUSTOMER y catálogo público con búsqueda (1:30).
3. Crear una orden, repetir el `Idempotency-Key`, mostrar descuentos y cancelar (2:00).
4. Como ADMIN, mostrar CRUD de productos, ajuste de inventario, reportes y auditoría (2:00).
5. Mostrar Swagger, migraciones Flyway, pruebas Testcontainers, JaCoCo y workflows de CI (2:00).
6. Cerrar con Docker, decisiones de seguridad y limitaciones conocidas (1:00).

Credenciales locales documentadas en README. El guion describe acciones reales; no se deben simular respuestas.
