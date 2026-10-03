## Why

The infrastructure runs, but there is no application to run on it. Every later block (publisher, classifier, writer, live feed) adds code to one Spring Boot app, so the app, its build and its shared message types have to exist first. See `docs/DECISIONS.md` (1-4).

## What Changes

- Add a single `pom.xml` (one module) with Spring Boot 4.1.1, Java 21, and the dependencies the app will use: Spring Kafka, JPA with PostgreSQL, Spring Web.
- The `pom.xml` states the toolchain it needs (Java 21, Maven 3.9) and fails the build early when it is not met.
- Add the main class `KafkaOnKafkaApplication` and an `application.yml` that connects to the local Docker infrastructure by default and can be overridden with environment variables.
- Add the `domain` package: `Quote`, `EmotionResult` and the `Emotion` enum, with one JSON test.
- Other packages (`application`, `adapter.*`) are not created now: they appear with their first class in blocks 3-6.
- No Maven Wrapper, no Dockerfile (block 7), no `@SpringBootTest`.

## Capabilities

### New Capabilities
- `maven-build`: the single-module build, the declared Java and Maven requirements and the Java 21 target.
- `domain-model`: the message types shared across the app and their JSON form.
- `app-config`: how the application starts and how it finds Kafka and Postgres.

### Modified Capabilities

## Impact

- New files: `pom.xml`, `src/main/java/com/kiovaz/kafkaonkafka/KafkaOnKafkaApplication.java`, `src/main/java/com/kiovaz/kafkaonkafka/domain/*`, `src/main/resources/application.yml`, `src/test/java/.../domain/JsonTest.java`.
- Needs network on first build (Maven Central).
- Needs JDK 21 and Maven 3.9 (already set up: Temurin 21 as the IntelliJ project SDK, IntelliJ's bundled Maven 3.9.16).
- Running the app needs the Docker infrastructure up (`docker compose up -d`).
