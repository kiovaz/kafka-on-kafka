## Context

No Java code exists yet. The README fixes the module layout (`pom.xml` at the root, `common/` with `Quote`, `EmotionResult`, `Emotion`) and the JSON examples. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- `mvn verify` at the root builds `common` and runs its tests.
- One Jackson configuration answer for `Instant` and `Emotion` that every service can reuse.

**Non-Goals:**
- Service modules, Dockerfiles, Spring Boot parent.
- A shared `ObjectMapper` factory class: nothing needs it until the first service does.

## Decisions

**Root pom is a plain parent, not the Spring Boot parent.** The producer and classifier are framework-free; Spring services will import `spring-boot-dependencies` as a BOM in their own modules when they exist. The root only sets `release` 21, UTF-8 and `dependencyManagement` for Jackson (BOM `jackson-bom`) and JUnit (BOM `junit-bom`).

**`Emotion` is a plain enum serialized as lowercase.** Constants are lowercase (`anxiety`, ..., `unknown`), so Jackson's default name-based mapping already produces the README format, with no annotations. Rejected: uppercase constants plus `@JsonValue` (extra code for the same output). The Java naming-convention cost is accepted; the README treats these as the stored text values.

**`Instant` as ISO-8601 via `jackson-datatype-jsr310`.** The JSON needs `2026-10-03T14:22:10Z`, so a consumer's `ObjectMapper` must register `JavaTimeModule` and disable `WRITE_DATES_AS_TIMESTAMPS`. That setup lives in the test now; each service repeats the two lines. Rejected: a shared helper in `common` for two lines.

**Tests run through Docker.** No JDK or Maven on the host, so the build command is `docker run --rm -v "$PWD":/w -w /w maven:3-eclipse-temurin-21 mvn verify`.

**Tests: one JUnit class** doing the three round trips from the spec, asserting on the JSON text for field names and the `Instant` format.

## Risks / Trade-offs

- [Services configure `ObjectMapper` differently and break the `Instant` format] → The test documents the required config; the writer and frontend (Spring) will also need `WRITE_DATES_AS_TIMESTAMPS` off, which Spring Boot does by default.
- [First Docker Maven run downloads dependencies slowly] → One-time; mount `~/.m2` only if it becomes annoying.
