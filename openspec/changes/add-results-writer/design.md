## Context

Results are published to `emotions` (message key = `part`, JSON `EmotionResult`). The database is Neon, reached with `DB_URL`, `DB_USER`, `DB_PASSWORD` from `.env` (decision 19). The global consumer settings already use `ErrorHandlingDeserializer` over `JacksonJsonDeserializer` with no type headers, and a global `DefaultErrorHandler` retries a failing message every 2 s without limit and skips undeserializable ones (decision 17). See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- Every result in `emotions` ends up as one row, once, even with duplicates and restarts.
- A database outage or a bad message never loses or blocks results.

**Non-Goals:**
- Reading the data back (queries, reports): the data is browsed in the Neon console.
- Migrations or schema versioning beyond one `CREATE TABLE IF NOT EXISTS`.
- Running anything against Kafka or Neon in this change.

## Decisions

**`JdbcClient` and plain SQL instead of JPA.** The only write is `INSERT ... ON CONFLICT DO NOTHING`, which JPA cannot express without a native query anyway, and nothing is ever read back. So no entity, no repository interface, no Hibernate. This replaces the JPA plan in decision 10 and the README; it also cuts startup time and memory on a machine that is short of both. Swap in `spring-boot-starter-jdbc` and drop the `spring.jpa` setting. Rejected: keeping `spring-boot-starter-data-jpa` for the sake of the original plan.

**Schema from `schema.sql` with `spring.sql.init.mode=always`.** Spring Boot runs `schema.sql` at startup; `CREATE TABLE IF NOT EXISTS` makes it safe to run every time. Rejected: Flyway or Liquibase (a tool for one table), and `ddl-auto` (JPA is gone).

**`emotion` is stored as text** (the lowercase JSON name, `result.emotion().json()`), `part` is nullable, `classified_at` is `TIMESTAMPTZ`. The Java value is passed as an `OffsetDateTime` in UTC, because the PostgreSQL driver does not accept `Instant` as a parameter.

**`ON CONFLICT DO NOTHING` has no conflict target.** It is valid in PostgreSQL and in H2 (PostgreSQL mode), and the only unique rule besides the generated id is (`book`, `quote_id`), so it means the same thing.

**First row wins.** `ON CONFLICT DO NOTHING` keeps the earliest result for a (book, quote) pair, which matches decision 10 and keeps a republished book harmless.

**Three small classes, no interfaces:** `adapter.in.kafka.ResultsListener` (`@KafkaListener(topics = "emotions", groupId = "results-writer")` with the default value type `EmotionResult` set on the listener, not globally, because the live feed listens to the same topic later), `application.SaveResult` (calls the repository and logs whether a row was added or ignored) and `adapter.out.persistence.ResultRepository` (the SQL).

**Failures use the existing global error handler:** the repository lets the `DataAccessException` propagate, the listener fails, and `DefaultErrorHandler` retries the same record every 2 s; a `DeserializationException` is skipped. Nothing new to configure, and the same behaviour as the classifier's publishing failure.

**Tests without Docker or Neon:** `SaveResultTest` with Mockito, and `ResultRepositoryTest` using H2 in PostgreSQL mode, loading the real `schema.sql` (insert, duplicate ignored, null part, emotion lowercase). H2 is a test-scope dependency managed by Spring Boot. If H2 cannot run the real script unchanged, the repository test is dropped and the SQL is proven in the final check against Neon instead (the plan prefers a real check over a diluted test).

## Risks / Trade-offs

- [H2 is not PostgreSQL, so a green test could hide a PostgreSQL-only problem] → The real SQL is proven against Neon in the final checks; the H2 test only catches mistakes early.
- [Neon scales to zero, so the first connection after idle can be slow or time out] → The connection pool retries on its own; startup waits for the schema script. Noted in the README.
- [The app now fails to start when the database is unreachable] → Accepted: the writer cannot work without it, and the failure is loud.
- [Moving off JPA changes a documented decision] → Recorded as a new decision and in the README.
