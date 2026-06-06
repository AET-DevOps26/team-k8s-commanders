#
# Multi-stage build for the api-gateway.
# Build from services/api-gateway as context:
#     docker build -t caredesk-api-gateway ./services/api-gateway

# Stage 1 — build the JAR
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2 — slim runtime image
FROM eclipse-temurin:21-jre-alpine
# Run as a non-root user for runtime security.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
