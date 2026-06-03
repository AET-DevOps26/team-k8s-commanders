# syntax=docker/dockerfile:1
#
# Multi-stage build for the auth-service.
#
# The auth-service depends on the OpenAPI-generated stubs JAR
# (`org.openapitools:openapi-spring:1.0.0`), which is NOT in Maven Central —
# it lives in `services/springboot/generated/` and must be built and installed
# into the local Maven repo before the auth-service can compile.
#
# Build from the REPO ROOT (not from services/auth-service):
#     docker build -f services/auth-service/Dockerfile -t caredesk-auth-service .


# Stage 1 — generate the OpenAPI Spring stubs from the checked-in API spec
# ---------------------------------------------------------------------------
# CI downloads services/springboot/generated as an artifact before building the
# image, but local Docker builds do not have that generated directory. Generate
# it inside the Docker build so `docker compose up --build` works from a clean
# checkout.
FROM node:24-alpine AS stubs-generator
RUN apk add --no-cache openjdk21-jre
WORKDIR /repo
COPY package.json package-lock.json* ./
RUN npm ci
COPY api/openapi.yaml api/openapi.yaml
RUN npx openapi-generator-cli generate \
    -i api/openapi.yaml \
    -g spring \
    -o services/springboot/generated \
    -p interfaceOnly=true,useSpringBoot3=true


# Stage 2 — build & install the OpenAPI stubs into a shared local Maven repo
# ---------------------------------------------------------------------------
# We deliberately do NOT run `mvn install` on the stubs project — that would
# trigger spring-boot-maven-plugin's repackage step and produce a Spring Boot
# fat JAR (nested BOOT-INF/), which is unusable as a library dependency.
# Instead we compile classes, strip the stub project's own application.properties
# (which would otherwise override the auth-service's application.yml and set
# server.port=8080), and install a plain classes JAR.
FROM maven:3.9-eclipse-temurin-21 AS stubs-builder
WORKDIR /stubs
COPY --from=stubs-generator /repo/services/springboot/generated/pom.xml .
COPY --from=stubs-generator /repo/services/springboot/generated/src ./src
# Compile classes only; skip Spring Boot repackage.
RUN mvn compile -q
# Remove the stub project's application.properties — it sets server.port=8080
# and would shadow the auth-service's application.yml at runtime.
RUN rm -f target/classes/application.properties
# Build a plain library JAR from the compiled classes and install it into ~/.m2.
RUN cd target/classes && jar cf /tmp/openapi-spring-1.0.0.jar . \
    && mvn install:install-file \
        -Dfile=/tmp/openapi-spring-1.0.0.jar \
        -DgroupId=org.openapitools \
        -DartifactId=openapi-spring \
        -Dversion=1.0.0 \
        -Dpackaging=jar \
        -q


# Stage 3 — build the auth-service JAR
FROM maven:3.9-eclipse-temurin-21 AS app-builder
WORKDIR /app
# Carry over the local Maven repo from stage 1 (contains the stubs JAR).
COPY --from=stubs-builder /root/.m2 /root/.m2
# Cache dependencies as a separate layer when pom.xml hasn't changed.
COPY services/auth-service/pom.xml .
RUN mvn dependency:go-offline -q
COPY services/auth-service/src ./src
RUN mvn package -DskipTests -q


# Stage 4 — slim runtime image (no JDK, no Maven)
FROM eclipse-temurin:21.0.11_10-jre-alpine
# Run as a non-root user for runtime security.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=app-builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
