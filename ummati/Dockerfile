# syntax=docker/dockerfile:1

# ---------- Étape 1 : build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Les dépendances changent bien moins souvent que le code : on les résout d'abord
# pour que le cache Docker survive à une simple modification de source.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Étape 2 : exécution ----------
# JRE seul (pas de JDK, pas de Maven) : image plus légère et surface d'attaque réduite.
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup -S ummati && adduser -S ummati -G ummati

WORKDIR /app

# Les images uploadées et les documents vivent ici ; en production ce chemin doit être
# monté sur un volume, sinon tout disparaît au redéploiement du conteneur.
RUN mkdir -p /app/uploads && chown -R ummati:ummati /app

COPY --from=build --chown=ummati:ummati /build/target/*.jar app.jar

USER ummati

ENV SPRING_PROFILES_ACTIVE=prod \
    UPLOAD_DIR=/app/uploads \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

EXPOSE 8080

# readiness (et non l'agrégat /health) : le conteneur est « prêt » quand la base
# répond. L'agrégat inclut des dépendances non vitales dont l'indisponibilité ne doit
# pas provoquer un redémarrage.
# 127.0.0.1 et non « localhost » : Tomcat écoute en IPv4 alors que la résolution de
# localhost dans l'image Alpine tente ::1 en premier.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
