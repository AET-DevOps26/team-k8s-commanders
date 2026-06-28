#
# Multi-stage build for the notification-service.
#
# Depends on the OpenAPI-generated stubs JAR (org.openapitools:openapi-spring)
# which is not in Maven Central. The stubs live in services/springboot/generated/
# (committed to the repo). Same pattern as auth-service and patient-service:
# compile the stubs project, strip its application.properties so it does not
# shadow our application.yml, install it as a plain library JAR, then build the
# service against it.
#
# Build from the REPO ROOT (not from services/notification-service):
#     docker build -f services/notification-service/Dockerfile -t caredesk-notification-service .

# ---------------------------------------------------------------------------
# Stage 1 — build & install the OpenAPI stubs into a shared local Maven repo
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS stubs-builder
WORKDIR /stubs
COPY services/springboot/generated/pom.xml .
COPY services/springboot/generated/src ./src
RUN mvn compile -q
RUN rm -f target/classes/application.properties
RUN cd target/classes && jar cf /tmp/openapi-spring-1.0.0.jar . \
    && mvn install:install-file \
        -Dfile=/tmp/openapi-spring-1.0.0.jar \
        -DgroupId=org.openapitools \
        -DartifactId=openapi-spring \
        -Dversion=1.0.0 \
        -Dpackaging=jar \
        -q

# ---------------------------------------------------------------------------
# Stage 2 — build the notification-service JAR
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS app-builder
WORKDIR /app
COPY --from=stubs-builder /root/.m2 /root/.m2
COPY services/notification-service/pom.xml .
RUN mvn dependency:go-offline -q
COPY services/notification-service/src ./src
RUN mvn package -DskipTests -q

# ---------------------------------------------------------------------------
# Stage 3 — slim runtime image
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=app-builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
