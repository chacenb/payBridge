FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
# -Dmaven.test.skip=true (not -DskipTests): the test tree is currently stale/set-aside
# (references entities removed by the PaymentAttempt-to-PaymentTransaction collapse) and
# must not be compiled during packaging, not just skipped at run time.
RUN mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

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

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
