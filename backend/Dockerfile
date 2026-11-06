# Build stage: compile the jar with the Maven Wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# dependencies first, so they are cached when only the source code changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
# tests need a running database, they are run outside of Docker
RUN ./mvnw -q package -DskipTests

# Run stage: only the JRE and the jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
