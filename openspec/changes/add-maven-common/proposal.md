## Why

The infrastructure runs, but there is no Java code. Every service (producer, classifier, writer, frontend) needs the same build setup and the same data models. This is block 2: the Maven skeleton and the shared `common` module.

## What Changes

- Add the root `pom.xml` (Java 21, packaging `pom`, dependency management for Jackson and JUnit) with `common` as its only module.
- Add the `common` module with the `Quote` and `EmotionResult` records and the `Emotion` enum, exactly as defined in the README.
- Add a JSON round-trip test for both records, so the wire format the services will share is pinned down.
- Not in this change: the service modules (added by the blocks that implement them), Dockerfiles, Spring Boot parent (only the Spring services need it, later).

## Capabilities

### New Capabilities
- `shared-models`: the `Quote`, `EmotionResult` and `Emotion` types and their JSON format.

### Modified Capabilities

## Impact

- New files only: `pom.xml`, `common/pom.xml`, `common/src/**`.
- New dependencies: `jackson-databind` + `jackson-datatype-jsr310` (for `Instant`), `junit-jupiter` (test scope).
- Java and Maven are not installed on the host, so building and testing run through the `maven:3-eclipse-temurin-21` Docker image.
