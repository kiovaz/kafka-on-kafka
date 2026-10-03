## Why

Nothing runs yet. Every later block (producer, classifier, writer, frontend) needs a broker with the right topics, a database to browse, and a book to stream. This is the base they all build on.

## What Changes

- Add `docker-compose.yml` with `kafka` (KRaft, one internal listener for containers and one external for host tools), `postgres` and `adminer`.
- Add a `kafka-init` container that creates the `quotes` and `emotions` topics (3 partitions each, `emotions` with unlimited retention) and exits.
- Add `data/metamorphosis.txt` (Project Gutenberg) and `data/book-config.json`.
- Not in this change: Ollama and its init container (block 4), Maven projects (block 2), service containers (block 7).

## Capabilities

### New Capabilities
- `local-infrastructure`: Kafka broker, topics, Postgres and Adminer started by Docker Compose.
- `book-source`: the book text and the config describing how the producer reads it.

### Modified Capabilities

## Impact

- New files only: `docker-compose.yml`, `data/metamorphosis.txt`, `data/book-config.json`.
- Host ports used: `29092` (Kafka), `5432` (Postgres), `8081` (Adminer).
- Docker images pulled: `apache/kafka`, `postgres:16`, `adminer`.
