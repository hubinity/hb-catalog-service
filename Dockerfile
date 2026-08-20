# syntax=docker/dockerfile:1.7
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21-alpine
ARG JRE_IMAGE=eclipse-temurin:21-jre-alpine

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace

# Cache mount id below is Railway's non-standard requirement: id=s/<service-id>-<path>,
# with the service id hardcoded (build args/env vars are rejected in this field:
# https://station.railway.com/questions/cache-mount-id-is-not-prefixed-with-cach-456d2f50).
# Plain `docker build` elsewhere treats it as an opaque string, so this stays portable.
# 9a2618b0-cdbc-4c4e-bd2a-7cc6acd1cd40 is hb-catalog-service's id in the "hubinity"
# Railway project — update it if this Dockerfile is ever reused for a different service.

# Shared contracts (vendored as a git submodule, pinned in platform-shared-contracts/).
# Fails fast and clearly if the builder didn't check out submodule content.
COPY platform-shared-contracts ./platform-shared-contracts
RUN ls platform-shared-contracts/pom.xml
RUN --mount=type=cache,id=s/9a2618b0-cdbc-4c4e-bd2a-7cc6acd1cd40-/root/.m2,target=/root/.m2 \
    mvn -B -ntp -q -f platform-shared-contracts/pom.xml \
    -pl contracts-catalog,contracts-events -am install -DskipTests

# Cache deps first
COPY pom.xml .
RUN --mount=type=cache,id=s/9a2618b0-cdbc-4c4e-bd2a-7cc6acd1cd40-/root/.m2,target=/root/.m2 mvn -B -ntp -q dependency:go-offline || true
COPY src ./src
RUN --mount=type=cache,id=s/9a2618b0-cdbc-4c4e-bd2a-7cc6acd1cd40-/root/.m2,target=/root/.m2 mvn -B -ntp -q -DskipTests package spring-boot:repackage

FROM ${JRE_IMAGE} AS runtime
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /workspace/target/hb-catalog-service-*.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
