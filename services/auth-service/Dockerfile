#
# Multi-stage build for the auth-service.
#
# The auth-service depends on the OpenAPI-generated stubs JAR
# (`org.openapitools:openapi-spring:1.0.0`), which is NOT in Maven Central —
# it lives in `services/springboot/generated/` (committed to the repo) and must
# be built and installed into the local Maven repo before the auth-service can
# compile.
#
# Build from the REPO ROOT (not from services/auth-service):
#     docker build -f services/auth-service/Dockerfile -t caredesk-auth-service .

# ---------------------------------------------------------------------------
# Stage 1 — build & install the OpenAPI stubs into a shared local Maven repo
# ---------------------------------------------------------------------------
# We deliberately do NOT run `mvn install` on the stubs project — that would
# trigger spring-boot-maven-plugin's repackage step and produce a Spring Boot
# fat JAR (nested BOOT-INF/), which is unusable as a library dependency.
# Instead we compile classes, strip the stub project's own application.properties
# (which would otherwise override the auth-service's application.yml and set
# server.port=8080), and install a plain classes JAR.
FROM maven:3.9-eclipse-temurin-25 AS stubs-builder
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
# Stage 2 — build the auth-service JAR
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS app-builder
WORKDIR /app
COPY --from=stubs-builder /root/.m2 /root/.m2
COPY pom.xml /pom.xml
COPY config/spotbugs/spotbugs-exclude.xml /config/spotbugs/spotbugs-exclude.xml
COPY services/auth-service/pom.xml .
RUN mvn dependency:go-offline -q
COPY services/auth-service/src ./src
RUN mvn package -DskipTests -q

# ---------------------------------------------------------------------------
# Stage 3 — slim runtime image (no JDK, no Maven)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25.0.3_9-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=app-builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
