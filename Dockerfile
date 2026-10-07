# --- build: compile and package the runnable jar (the tests run on the host, not here)
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# --- run: only a JRE and the jar
FROM eclipse-temurin:21.0.12_8-jre
RUN useradd --system --no-create-home app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
