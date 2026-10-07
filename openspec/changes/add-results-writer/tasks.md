> **Status:** the code and its tests are written. Nothing has been compiled or run yet (decision of the owner: run everything once all blocks are coded), so no task below is ticked until it is verified in that run.

## 1. Dependencies and configuration

- [ ] 1.1 In `pom.xml` replace `spring-boot-starter-data-jpa` with `spring-boot-starter-jdbc`, add `h2` with test scope, remove the `spring.jpa` setting and add `spring.sql.init.mode: always` in `application.yml`, and verify `mvn test` still compiles and passes

## 2. Storage

- [ ] 2.1 Add `src/main/resources/schema.sql` (`CREATE TABLE IF NOT EXISTS classified_quotes` with the columns and the `UNIQUE (book, quote_id)` rule of the `results-storage` spec)
- [ ] 2.2 Add `ResultRepository` in `adapter.out.persistence` (`JdbcClient`, `INSERT ... ON CONFLICT DO NOTHING`, `OffsetDateTime` in UTC, returns whether a row was added) and `ResultRepositoryTest` over H2 in PostgreSQL mode with the real `schema.sql` (new row, duplicate ignored and the first kept, null part, lowercase emotion, running the script twice), and verify the tests pass

## 3. Use case and listener

- [ ] 3.1 Add `SaveResult` in `application` and `SaveResultTest` with Mockito (calls the repository once with the result, logs added or ignored), and verify the tests pass
- [ ] 3.2 Add `ResultsListener` in `adapter.in.kafka` (group `results-writer`, default value type `EmotionResult` on the listener) and verify `mvn test` passes

## 4. Documentation

- [ ] 4.1 Update the README (persistence is `JdbcClient` and plain SQL, the table, the writer's group, the Neon wake-up note) and add decision 22 to `docs/DECISIONS.md` (no JPA), and run `openspec validate add-results-writer`

## 5. Final checks (run at the end of the whole development; need Docker, Kafka and Neon)

- [ ] 5.1 Start the stack and the app and verify rows appear in the Neon console for every classified quote, with lowercase emotions and the right parts
- [ ] 5.2 Restart the app so the whole book is classified again and verify the row count does not grow
- [ ] 5.3 Verify `kafka-consumer-groups.sh --describe --group results-writer` shows the group with lag 0
- [ ] 5.4 Stop the writer while results keep arriving, start it again and verify it writes the missed results
- [ ] 5.5 Publish a message that is not valid JSON to `emotions` between two valid results and verify it is skipped with one log entry and both valid results are written
- [ ] 5.6 Make the database unreachable for a while (wrong `DB_URL`, or network off) while results arrive, restore it and verify nothing is missing
