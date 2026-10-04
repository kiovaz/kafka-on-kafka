## 1. Maven skeleton

- [ ] 1.1 Add root `pom.xml` (packaging `pom`, Java 21, UTF-8, `jackson-bom` and `junit-bom` in `dependencyManagement`, module `common`) and verify `docker run --rm -v "$PWD":/w -w /w maven:3-eclipse-temurin-21 mvn -q validate` succeeds

## 2. Shared models

- [ ] 2.1 Add `common/pom.xml` (jackson-databind and jsr310 compile, junit-jupiter test) and the `Emotion` enum, `Quote` and `EmotionResult` records under `common/src/main/java`, and verify `mvn -q compile` succeeds
- [ ] 2.2 Add one JUnit test class covering the spec scenarios (Quote with and without `part`, Emotion lowercase, EmotionResult with ISO-8601 `classifiedAt`) and verify `mvn verify` passes with the tests executed

## 3. Final check

- [ ] 3.1 Run `mvn verify` from a clean checkout (`mvn clean verify`) and verify it passes, then run `openspec validate add-maven-common` and verify it passes
