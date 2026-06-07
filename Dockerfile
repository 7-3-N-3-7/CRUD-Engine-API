# Stage 1: Build the application using Maven and Java 25
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy parent and all sub-modules POM files
COPY pom.xml .
COPY crud-engine-core/pom.xml crud-engine-core/
COPY crud-engine-jpa/pom.xml crud-engine-jpa/
COPY crud-engine-inmemory/pom.xml crud-engine-inmemory/
COPY crud-engine-weaviate/pom.xml crud-engine-weaviate/
COPY crud-engine-webflux/pom.xml crud-engine-webflux/
COPY crud-engine-security-keycloak/pom.xml crud-engine-security-keycloak/
COPY crud-engine-plugin-ratelimiter/pom.xml crud-engine-plugin-ratelimiter/
COPY crud-engine-plugin-auditlog/pom.xml crud-engine-plugin-auditlog/
COPY crud-engine-spring-boot-starter/pom.xml crud-engine-spring-boot-starter/
COPY crud-app-sample/pom.xml crud-app-sample/

# Copy all source folders
COPY crud-engine-core/src crud-engine-core/src
COPY crud-engine-jpa/src crud-engine-jpa/src
COPY crud-engine-inmemory/src crud-engine-inmemory/src
COPY crud-engine-weaviate/src crud-engine-weaviate/src
COPY crud-engine-webflux/src crud-engine-webflux/src
COPY crud-engine-security-keycloak/src crud-engine-security-keycloak/src
COPY crud-engine-plugin-ratelimiter/src crud-engine-plugin-ratelimiter/src
COPY crud-engine-plugin-auditlog/src crud-engine-plugin-auditlog/src
COPY crud-engine-spring-boot-starter/src crud-engine-spring-boot-starter/src
COPY crud-app-sample/src crud-app-sample/src

# Compile and package only the executable sample module
RUN mvn clean package -pl crud-app-sample -am -DskipTests -B

# Stage 2: Minimal runtime image with JRE 25
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copy executable jar from build stage
COPY --from=build /app/crud-app-sample/target/crud-app-sample-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Configure memory limits optimized for 1 CPU Core / 4GB RAM VPS
ENTRYPOINT ["java", "-XX:ActiveProcessorCount=1", "-Xms256m", "-Xmx768m", "-jar", "app.jar"]
