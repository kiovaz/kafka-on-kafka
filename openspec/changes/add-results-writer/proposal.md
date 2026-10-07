## Why

The classified results only live in the `emotions` topic. The writer saves them in PostgreSQL (Neon) so they can be queried with SQL, and it is the second consumer group on the same topic, which is the Kafka lesson of the project. See `docs/DECISIONS.md` (6, 10, 19).

## What Changes

- Add `schema.sql` that creates the table `classified_quotes` if it does not exist, run at every startup.
- Add `ResultRepository` (`adapter.out.persistence`): one `INSERT ... ON CONFLICT DO NOTHING` through Spring's `JdbcClient`.
- Add `SaveResult` (`application`) and `ResultsListener` (`adapter.in.kafka`), consumer group `results-writer` on `emotions`.
- Replace `spring-boot-starter-data-jpa` with `spring-boot-starter-jdbc` (no entity, no Hibernate: the only write is one native statement, so JPA adds nothing) and add H2 as a test dependency. Remove the `spring.jpa` setting.
- Tests: `SaveResult` with Mockito, and `ResultRepository` against H2 in PostgreSQL mode using the same `schema.sql`.
- Update the README and `docs/DECISIONS.md`.
- Nothing is run against Kafka or Neon in this change; the end-to-end checks are listed as the last tasks and are done at the end of the whole development.

## Capabilities

### New Capabilities
- `results-storage`: the table, how results are written, duplicates, and what happens when the database or a message is bad.

### Modified Capabilities

## Impact

- New: `schema.sql`, `ResultRepository`, `SaveResult`, `ResultsListener`, tests. Changed: `pom.xml`, `application.yml`.
- The data lives in the Neon database named by `DB_URL`, `DB_USER` and `DB_PASSWORD` (decision 19).
- The app now needs the database at startup (the schema script runs then), and Neon may take a few seconds to wake up after being idle.
