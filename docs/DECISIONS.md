# Architecture Decisions

Short record of what was decided, why, and what was rejected. Newest decisions go at the bottom. All dated 2026-10-03 unless noted.

## 1. One Spring Boot application, one Maven module

**Decision:** the whole pipeline (publisher, classifier, results writer, live feed) is a single Spring Boot app with one `pom.xml` and one `src`.

**Why:** the four pieces are small, and one app is much easier to read, build and run. The Kafka concepts (topics, partitions, consumer groups, offsets, lag) still apply because each piece is its own listener with its own consumer group.

**Rejected:** five Maven modules (`common` + four services) to keep plain-Kafka and Spring code apart. It was built and thrown away: five `pom.xml` files and a shared module for a very small codebase. Search results on hexagonal Java projects also recommend staying single-module for small projects.

## 2. Hexagonal package layout, but no interface without a second implementation

**Decision:** packages follow `domain` / `application` / `adapter.in` / `adapter.out`. Classes call each other directly; an interface is created only when a second implementation appears.

**Why:** the layout keeps the code organized by role. An interface with a single implementation (a "port") only adds a file. Tests can simulate a class with Mockito.

**Rejected:** a port (interface) for every external system (Kafka, Ollama, Postgres, the book file). Add one later if a second classifier ever exists.

## 3. Spring Kafka for everything

**Decision:** `@KafkaListener` and `KafkaTemplate` everywhere.

**Why:** with one Spring app, mixing in the raw `kafka-clients` API would add code without a reason.

**Rejected:** plain `kafka-clients` for the publisher and the classifier, to see the raw polling and offset mechanics. Dropped together with decision 1.

## 4. Spring Boot 4.1.1 and Java 21

**Decision:** Spring Boot 4.1.1 (the latest stable; the `latest` tag on Maven Central is a milestone) on Java 21 (LTS). Kafka and Jackson versions come from Spring Boot's management; Spring Boot 4 uses Jackson 3 (`tools.jackson`).

**Why:** one source of versions, no overrides. The Kafka broker is 4.3.1 and the managed client is 4.2.1, which is compatible.

**Rejected:** a Maven Wrapper (the Maven comes from IntelliJ or an installation, as the user prefers), and overriding Boot's Kafka version.

## 5. Two topics, 3 partitions each, keyed by `part`

**Decision:** `quotes` (raw excerpts) and `emotions` (classified results), 3 partitions each, message key = `part`. `emotions` keeps its messages forever (`retention.ms=-1`).

**Why:** two topics decouple classifying from consuming the results. The key keeps the excerpts of one part in order. Unlimited retention lets the live feed replay the topic.

**Note:** a book has only 3 parts, so Kafka's key hashing may put two parts on one partition and leave another empty. That is a feature to observe, not a bug.

## 6. Three consumer groups

**Decision:** `classifier` (reads `quotes`), `results-writer` and `frontend` (both read `emotions`).

**Why:** separate groups let two consumers get every classified result independently, which is the core Kafka lesson of the project.

## 7. The publisher reads the book at startup

**Decision:** the app reads `data/metamorphosis.txt` and `data/book-config.json` and splits the text into paragraphs itself.

**Why:** the config file already says which text to use and how to chunk it. Switching books needs no code change and no generated file to keep in sync.

**Rejected:** a pre-generated `quotes.json` produced by a script.

## 8. A classification never blocks the topic

**Decision:** the classifier cleans the model's answer; anything outside the 7 emotions becomes `unknown`. If the Ollama call fails, it retries 3 times, then also records `unknown`. The classifier listener uses `max.poll.records=1`.

**Why:** a consumer that fails forever on one message never advances its offset, so everything behind it waits. One message per poll also keeps a slow model below `max.poll.interval.ms` (5 minutes).

## 9. The live feed reads only from Kafka and replays the topic

**Decision:** on startup the live feed reads `emotions` from the beginning and rebuilds the counts and the latest results in memory. `GET /stats` returns that state; `/events` (Server-Sent Events) pushes new results. The page is a static file in `src/main/resources/static/`; the endpoints live in `adapter.in.web`.

**Why:** reloading the page must not reset the counts, and the frontend should not depend on the database. Replay is one of the Kafka features the project exists to show.

**Rejected:** having the frontend query PostgreSQL for the initial state.

## 10. PostgreSQL stores the history, browsable with Adminer

**Decision:** the results writer saves each result to `classified_quotes` with `UNIQUE (book, quote_id)` and `ON CONFLICT DO NOTHING`. The emotion is stored as text. Adminer (port 8081) lets you browse the data.

**Why:** Kafka delivers at least once and a restart republishes the book, so duplicates must be harmless. A database nobody can look at has no value, hence Adminer. The Java `Emotion` enum already enforces the allowed values, so a PostgreSQL ENUM type would only complicate JPA.

**Rejected:** a PostgreSQL ENUM, and the frontend reading from this table.

## 11. Everything runs in Docker

**Decision:** Docker Compose runs Kafka (KRaft), Postgres, Adminer, Ollama and the app. Services reach each other by name (`kafka:9092`, `ollama:11434`). Kafka has an internal listener (`kafka:9092`) and an external one (`localhost:29092`) for host tools. Kafka and Postgres data live in named volumes. Image versions are pinned (`apache/kafka:4.3.1`, `adminer:6.1.1`, `postgres:16`). One-shot containers create the topics (`kafka-init`) and pull the model (`ollama-init`).

**Why:** one command brings everything up, reproducibly. Inside a container `localhost` is the container itself, so service names are required. Volumes keep topics and messages across `docker compose down`.

## 12. Specs and decisions are versioned; the AI tool's files are not

**Decision:** `openspec/` (main specs and the archive of finished changes) is committed. `.claude/`, `.idea/` and `TODO.md` stay local and ignored.

**Why:** the specs and their design notes record why things were built; the others are personal tooling.

**Note:** specs and the README describe some of the same things. The README is the overview; the specs are the contract for each part.
