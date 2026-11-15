# syntax=docker/dockerfile:1

# --- Construction -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace

# Les dépendances sont mises en cache tant que le pom.xml ne change pas.
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# --- Exécution --------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S fastsearch && adduser -S -G fastsearch fastsearch

WORKDIR /app
COPY --from=build /workspace/target/fastsearch-persons.jar /app/app.jar
RUN mkdir -p /app/data && chown -R fastsearch:fastsearch /app

USER fastsearch
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q -O - http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
