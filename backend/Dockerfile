# --- Étape de compilation -------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# Le wrapper Maven télécharge une distribution au format ZIP.
RUN apk add --no-cache unzip

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -q -DskipTests package \
    && find target -maxdepth 1 -name 'kyc-*.jar' ! -name '*.original' -exec cp {} /build/app.jar \;

# --- Étape d'exécution ----------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S kyc && adduser -S kyc -G kyc

WORKDIR /app
COPY --from=build /build/app.jar /app/app.jar
RUN mkdir -p /app/data/documents && chown -R kyc:kyc /app

USER kyc
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
