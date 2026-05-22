# Multi-stage build — context must be repository root: docker build -f query-service/Dockerfile .
# Use jammy (not alpine): eclipse-temurin:*-alpine has no linux/arm64 manifest (Apple Silicon / Colima).
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY rag-common ./rag-common
COPY query-service ./query-service

RUN chmod +x gradlew \
    && ./gradlew :query-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/query-service/build/libs/query-service-*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --retries=5 --start-period=90s \
  CMD curl -fsS http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
