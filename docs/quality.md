# Calidad

La calidad se valida con gates reproducibles en backend y frontend.

## Backend

Comando principal:

```bash
cd backend
mvn clean verify
```

El proceso incluye:

- JUnit 5;
- Mockito;
- pruebas de integración con PostgreSQL/Testcontainers;
- JaCoCo;
- Spotless;
- PMD.

El reporte HTML de cobertura queda en:

```text
backend/target/site/jacoco/
```

La cobertura válida es la generada por JaCoCo en la ejecución actual; no se documenta un porcentaje sin revisar el reporte producido por el build.

## Frontend

```bash
cd frontend
npm ci
npm run lint
npm test -- --watch=false
npm run build
```

Estos comandos coinciden con el workflow de CI actual.

## Principios

- no usar `--force` o `--legacy-peer-deps` como solución permanente;
- no sustituir PostgreSQL por H2 en pruebas de integración;
- no desactivar Flyway o `ddl-auto=validate` para ocultar divergencias;
- no añadir pruebas artificiales solo para inflar cobertura.

## SonarQube

Es opcional y no forma parte del gate obligatorio:

```bash
mvn verify sonar:sonar \
  -Dsonar.host.url=<url> \
  -Dsonar.token=<token>
```
