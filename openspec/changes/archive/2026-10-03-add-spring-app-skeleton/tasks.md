## 1. Build

- [x] 1.1 Add the root `pom.xml` (parent `spring-boot-starter-parent` 4.1.1, group `com.kiovaz.kafkaonkafka`, `java.version` 21, enforcer rule for Java 21+ and Maven 3.9+, the five dependencies, `spring-boot-maven-plugin`) and verify `mvn -q validate` succeeds with JDK 21 and IntelliJ's bundled Maven 3.9.16

## 2. Domain

- [x] 2.1 Add `Emotion` (uppercase constants, `@JsonValue` lowercase, includes `UNKNOWN`), `Quote` and `EmotionResult` in `domain`, and verify `mvn compile` succeeds
- [x] 2.2 Add one JUnit test covering the `domain-model` scenarios (lowercase emotion, `unknown`, rejected `"joy"`, EmotionResult JSON form with `classifiedAt`, round trips with null `part`), verify `mvn test` passes, and verify it fails when `@JsonValue` is removed
- [x] 2.3 Verify no class in `domain` imports Spring, Kafka, persistence or HTTP libraries (search the imports)

## 3. Application

- [x] 3.1 Add `KafkaOnKafkaApplication` (`@SpringBootApplication`) and `application.yml` with the local defaults and the `KAFKA_BOOTSTRAP_SERVERS`, `DB_URL`, `DB_USER`, `DB_PASSWORD` overrides, and verify `mvn verify` passes

## 4. Checks

- [x] 4.1 Run `mvn verify` with JDK 21 and verify one project is built and the tests pass
- [x] 4.2 Run the build with a JDK 17 and verify it fails before compiling with a message that Java 21 or newer is required
- [x] 4.3 Verify a compiled class has class-file major version 65 (read from the `.class` header)
- [x] 4.4 With `docker compose up -d` running, start the app with `mvn spring-boot:run`, verify it starts without errors and answers on `http://localhost:8080` (a 404 page counts), then stop it
- [x] 4.5 Start the app with `DB_URL` pointing to a wrong port and verify the error shows that URL (the `KAFKA_BOOTSTRAP_SERVERS` override uses the same placeholder pattern but is only observable once the first Kafka client exists, so it is checked in block 3)
- [x] 4.6 Verify `git status` shows no `target/` output after the build, then run `openspec validate add-spring-app-skeleton`
