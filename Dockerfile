# Stage 1: Build the application using Maven and Java 25
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy pom.xml and cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy sources and package jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime image with JRE 25
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copy executable jar from build stage
COPY --from=build /app/target/crudapp-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
