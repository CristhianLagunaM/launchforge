# Calidad

`mvn clean verify` ejecuta pruebas JUnit 5/Mockito, integraciones PostgreSQL con Testcontainers, JaCoCo, Spotless y PMD. El reporte HTML queda en `backend/target/site/jacoco`; el build aplica el umbral configurado y no se agregan pruebas artificiales.

En frontend, `npm run lint`, `npm run test -- --watch=false`, `npm run build` y `npm run format:check` son los gates. SonarQube es opcional: `mvn verify sonar:sonar -Dsonar.host.url=... -Dsonar.token=...`.
