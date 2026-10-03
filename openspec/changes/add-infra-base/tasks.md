## 1. Book source

- [x] 1.1 Download *The Metamorphosis* (Project Gutenberg plain text) to `data/metamorphosis.txt` and verify the START and END marker lines are present, start before end
- [x] 1.2 Write `data/book-config.json` with the 7 fields, setting `sectionPattern` after checking the real section lines in the text, and verify with a one-off script that the file parses, both markers match a line, and the pattern matches at least one line between them

## 2. Compose: Kafka

- [x] 2.1 Add the `kafka` service (`apache/kafka`, KRaft, `INTERNAL` `kafka:9092` + `EXTERNAL` `localhost:29092`, healthcheck) and verify `docker compose up -d kafka` reports healthy
- [x] 2.2 Add `kafka-init` creating `quotes` and `emotions` (3 partitions, `emotions` with `retention.ms=-1`, `--if-not-exists`) and verify `kafka-topics.sh --describe` shows both topics with 3 partitions and the retention setting
- [x] 2.3 Verify the host can reach the broker on `localhost:29092` and a container can reach `kafka:9092`

## 3. Compose: Postgres and Adminer

- [x] 3.1 Add `postgres` (`postgres:16`, port 5432, named volume, credentials set in the file) and verify `pg_isready` succeeds
- [x] 3.2 Add `adminer` on 8081 and verify logging in at `http://localhost:8081` (PostgreSQL / `postgres`) shows the database

## 4. Whole-stack check

- [x] 4.1 Run `docker compose up -d` from a clean state, then run it again, and verify nothing fails and the topics are unchanged
- [x] 4.2 Run `docker compose down` then `up`, and verify data created in Postgres before is still there
- [x] 4.3 Run `openspec validate add-infra-base` and verify it passes
