FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S launchforge && adduser -S launchforge -G launchforge
WORKDIR /app
COPY --from=build /workspace/target/launchforge-backend-*.jar app.jar
USER launchforge
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

