#
# Multi-stage build for the api-gateway.
#
# Build from the REPO ROOT (not from services/api-gateway) so the build can reach
# api/openapi.yaml — the gateway bundles the unified spec and serves it (plus a
# self-hosted Swagger UI) at /api/v1/openapi.yaml and /api/v1/docs.
#     docker build -f services/api-gateway/Dockerfile -t caredesk-api-gateway .

# Stage 1 — build the JAR
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml /pom.xml
COPY config/spotbugs/spotbugs-exclude.xml /config/spotbugs/spotbugs-exclude.xml
COPY services/api-gateway/pom.xml .
RUN mvn dependency:go-offline -q
COPY services/api-gateway/src ./src
# Bundle the unified OpenAPI spec as a static resource. api/openapi.yaml stays the
# single source of truth (no committed duplicate); the publish workflow rebuilds
# this image whenever the spec changes.
COPY api/openapi.yaml ./src/main/resources/static/api/v1/openapi.yaml
RUN mvn package -DskipTests -q

# Stage 2 — slim runtime image
FROM eclipse-temurin:25-jre-alpine
# Run as a non-root user for runtime security.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
