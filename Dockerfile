# =========================================================
# STAGE 1: BUILD - compile JAR with Maven + Java 21
# =========================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom.xml first (cache dependency layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Then copy source
COPY src ./src
# Build JAR (skip tests in container build)
RUN mvn clean package -DskipTests
# =========================================================
# STAGE 2: RUNTIME - small JRE-only image
# =========================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built JAR from stage 1
COPY --from=build /app/target/*.jar app.jar
# Document app port (doesn't publish - compose handles that)
EXPOSE 8080
# Launch Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]