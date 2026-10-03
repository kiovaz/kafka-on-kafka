## Context

Only infrastructure exists. The README and `docs/DECISIONS.md` fix the shape: one Spring Boot app, one Maven module, hexagonal package layout, Spring Kafka, no interface without a second implementation. See proposal.md for scope.

Facts checked on this machine and on Maven Central: Temurin 21 is installed and set as the IntelliJ project SDK; IntelliJ's bundled Maven is 3.9.16 (`plugins/maven-plugin/lib/maven3`); no `mvn` on the PATH. The newest stable Spring Boot is 4.1.1 (the `latest` tag, 4.2.0-M2, is a milestone). It manages Spring Kafka 4.1.1, Jackson 3 and `maven-enforcer-plugin` 3.6.3. In Spring Boot 4 the Kafka auto-configuration moved into `spring-boot-starter-kafka` and the web starter for servlet apps is `spring-boot-starter-webmvc` (both exist at 4.1.1).

## Goals / Non-Goals

**Goals:**
- `mvn verify` builds and tests the app with the IDE's Maven and JDK 21.
- The `pom.xml` itself says which Java and Maven the project needs.
- The app starts from the IDE against the Docker infrastructure.
- The shared message types exist with one runnable check of their JSON.

**Non-Goals:**
- A Maven Wrapper.
- Any listener, publisher, use case or controller (blocks 3-6), the Dockerfile (block 7), `schema.sql` and entities (block 5).
- A `@SpringBootTest`: it would need Kafka and Postgres running during `mvn verify`.

## Decisions

**Parent = `spring-boot-starter-parent` 4.1.1.** Version management for Spring, Kafka and Jackson, plugin defaults and UTF-8, with no version numbers on dependencies. Alternative: import the `spring-boot-dependencies` BOM into our own parent; rejected, it adds XML for nothing in a single module.

**Dependencies: `spring-boot-starter-kafka`, `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`, `postgresql` (runtime), `spring-boot-starter-test` (test).** They are all certain to be used in blocks 3-6, and adding them now lets the build and the startup check prove the Spring Boot 4 starter names and wiring resolve. Normally ponytail adds a dependency when its first use arrives; here it is one line each either way, so the early check wins. Jackson 3 comes through the web starter, no explicit dependency.

**Java and Maven requirements via `maven-enforcer-plugin`**: `requireJavaVersion [21,)` and `requireMavenVersion [3.9.0,)`, bound to `validate`, version from Boot's `${maven-enforcer-plugin.version}`. It makes the pom state what the project needs and fails with a clear message instead of a compiler error. Alternative: only `java.version` (sets the target but doesn't reject an old JDK clearly). It is the only addition beyond the bare minimum; remove it if it ever gets in the way.

**`spring-boot-maven-plugin` declared**, so `mvn spring-boot:run` works and block 7 can build the runnable jar.

**Java 21 via `java.version=21`** (sets the compiler `release`, so class files are version 65 even on a newer JDK and newer APIs are rejected).

**Only `domain` is created now.** Java and Git don't need empty packages; `application` and `adapter.*` appear with their first class. The README already documents the layout.

**`application.yml` with environment overrides**: `spring.kafka.bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}`, `spring.datasource.url: ${DB_URL:jdbc:postgresql://localhost:5432/kafka}`, user and password with the same pattern (`DB_USER`, `DB_PASSWORD`, default `kafka`). Defaults match the Docker infrastructure seen from the host (the IDE development loop); Docker sets the variables to the service names (block 7).

**`Emotion` is a Java enum with uppercase constants and `@JsonValue` returning the lowercase name**, so JSON and database text match the README and anything else is rejected. `UNKNOWN` is part of the enum. The `parse` method for the model's raw answer comes with the classifier in block 4.

**One test**: JSON round trips and the lowercase/rejection rules. The records and the enum are trivial, so nothing else is tested.

**Naming:** group id and base package `com.kiovaz.kafkaonkafka` (assumed from the GitHub user; easy to rename now, hard later).

## Risks / Trade-offs

- [Jackson 3 API differs from what most examples show] → JSON code is limited to one test; check class names against the real jar.
- [App fails to start if Postgres or Kafka are down] → The startup check runs with `docker compose up -d`; Kafka connects lazily, so a missing broker only shows up when listeners exist (block 3+).
- [Dependencies unused until later blocks] → Accepted, see Decisions.
- [IntelliJ may not pick up the root pom automatically] → Right-click `pom.xml` → *Add as Maven Project* (a one-time user step).
- [First build needs network] → Accepted; later builds use the local cache.
