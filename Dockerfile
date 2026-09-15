FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 paybridge \
    && mkdir -p /app/logs \
    && chown -R paybridge:paybridge /app

WORKDIR /app
COPY --from=build --chown=paybridge:paybridge /workspace/target/payBridge-0.0.1-SNAPSHOT.jar /app/app.jar

USER paybridge

EXPOSE 9000

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD curl --fail --silent http://localhost:9000/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
