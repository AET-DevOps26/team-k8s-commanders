# syntax=docker/dockerfile:1
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
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
