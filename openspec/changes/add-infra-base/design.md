## Context

Nothing exists yet besides the README. The README fixes the topology (see `README.md`: Kafka Setup, Docker Compose — Services). Everything runs in Docker, so containers reach each other by service name and the host only needs a few ports. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- One `docker compose up` brings up a working broker, topics, Postgres and Adminer.
- Safe to run repeatedly.

**Non-Goals:**
- Ollama, the Java services and their Dockerfiles (later blocks).
- Production hardening: single broker, no auth, no TLS.

## Decisions

**Single `docker-compose.yml`, grown block by block.** Later blocks add services to the same file instead of recreating it. No profiles or override files until they're needed.

**Kafka image: `apache/kafka`.** Official image, KRaft out of the box, and the CLI scripts live at `/opt/kafka/bin/` (checked in the image). Alternatives (Bitnami, Confluent) add nothing for a single local broker.

**Two listeners.** `INTERNAL` on `kafka:9092` for containers, `EXTERNAL` on `localhost:29092` for host tools. A single listener can't be both: advertising `localhost` breaks containers, advertising `kafka` breaks the host.

**Topics created by a one-shot `kafka-init` container** using `kafka-topics.sh --create --if-not-exists`, with `depends_on: kafka: condition: service_healthy`. Rejected: auto-create topics (defaults to 1 partition and no per-topic retention) and manual creation (not reproducible).

**Kafka data lives in a named volume** (`kafkadata`) mounted at `/var/lib/kafka/data`, with `KAFKA_LOG_DIRS` pointing there. The image's default log dir is `/tmp/kafka-logs`, which is lost on `down`; without the volume `emotions` could not keep its history across restarts, which the replay-based frontend depends on.

**Kafka healthcheck** runs `kafka-topics.sh --bootstrap-server localhost:9092 --list`, so `kafka-init` waits for a broker that actually answers.

**Postgres credentials via defaults in the compose file** (`POSTGRES_USER/PASSWORD/DB` set to `kafka`), no `.env` file. This is a local learning stack; a `.env` can come later without changing the services. Data lives in a named volume so it survives `down`.

**Adminer** is the stock image on 8081 (8080 is reserved for the frontend), with no extra config.

**Book files are plain data under `data/`.** The producer container will mount `data/` read-only in a later block; this change only creates the files. `sectionPattern` is `^(I|II|III)$`, matching the roman-numeral lines that open each part in the Gutenberg text, and is verified against the real file.

## Risks / Trade-offs

- [Gutenberg text layout differs from what `sectionPattern` assumes] → Check the real file when creating it and adjust the pattern (the spec requires it to match at least one line).
- [Host ports 5432, 8081 or 29092 already in use on the dev machine] → Compose fails to start that service; change the host side of the mapping.
- [Kafka healthcheck is slow on first start] → `start_period` and `retries` on the healthcheck; `kafka-init` simply waits.
- [Default credentials in the repo] → Acceptable for local use; documented as such.
