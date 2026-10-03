# Kafka-on-Kafka

## Overview

The goal is to put the core Kafka concepts into practice — producers, consumers, topics, partitions, consumer groups, offsets and lag — with a small but complete pipeline.

A producer publishes excerpts from *The Metamorphosis*, one at a time with a delay between them, simulating a continuous stream. A consumer reads each excerpt and sends it to a local LLM (Ollama, `qwen2.5:7b`), which classifies the excerpt's emotional tone (anxiety, absurdity, resignation, alienation, bureaucracy, despair, confusion). The result is published to a second topic, then stored and shown live on a small real-time frontend.

The producer and the classifier use the plain `kafka-clients` API so the Kafka mechanics stay visible; the results-writer and the frontend use Spring Boot to skip boilerplate that isn't core to learning Kafka.

## Scope

**In scope:**

- Book: *The Metamorphosis*, public-domain English translation (Project Gutenberg)
- Data and code language: English
- Java 21 (LTS), using `record` for data models
- Book-agnostic text source: `book-config.json` defines the book's text file, where it starts and ends, how sections are detected and how it is chunked — switching books requires no code changes (the 8 emotion categories are fixed and tuned for Kafka's themes)
- Two Kafka topics: one for raw excerpts, one for classified results
- Results stored in PostgreSQL
- Simple, real-time frontend (HTML/JS + Server-Sent Events) showing classifications as they happen
- Fully local execution, everything orchestrated via Docker Compose (Kafka, Postgres, Ollama and all the Java services)

**Out of scope (for now):**

- Other books by Franz Kafka (*The Trial*, *The Castle*) — the pipeline supports them through `book-config.json`, but they're not part of this first version
- Authentication/authorization on the frontend
- Kafka Streams or Kafka Connect (a possible next step)
- Multi-broker / a real Kafka cluster

## Architecture

```mermaid
flowchart LR
    P[Producer<br/><small>Java, kafka-clients</small>] --> T1[Topic<br/><small>quotes</small>]
    T1 --> C[Classifier Consumer<br/><small>Java, kafka-clients</small>]
    C -->|calls local Ollama| O[(Ollama<br/>qwen2.5:7b)]
    C --> T2[Topic<br/><small>emotions</small>]
    T2 --> W[Results Writer<br/><small>Spring Boot + Spring Kafka + JPA</small>]
    T2 --> FE[Frontend Service<br/><small>Spring Boot + Spring Kafka + SSE</small>]
    W --> PG[(PostgreSQL)]
    FE -->|Server-Sent Events| BR[Browser]
```

**Flow:** at startup, the producer reads the book's text file and `book-config.json`, splits the text into paragraphs and publishes them one at a time to `quotes` (by default every 3 seconds). The classifier consumer reads that topic one message at a time, calls Ollama's HTTP API to classify the excerpt's emotional tone, and publishes the result to `emotions`. Two independent consumers then read that second topic, each in its own consumer group: the results-writer saves the result to PostgreSQL, and the frontend streams it to the browser in real time via Server-Sent Events.

The producer never waits for the classifier. If the model is slower than the producer, excerpts pile up in the `quotes` topic (consumer lag) and the classifier catches up later — nothing is lost.

## Tech Stack

| Component | Technology | Role |
| --- | --- | --- |
| Language | Java 21 (LTS) | All services |
| Build | Maven (multi-module) | Dependency management and build |
| Shared models | `common` module | `Quote`, `EmotionResult` and `Emotion`, used by every service |
| Producer | Plain Java, `kafka-clients` | Splits the book and publishes excerpts — framework-free to learn the raw producer API |
| Classifier consumer | Plain Java, `kafka-clients` + `java.net.http.HttpClient` | Reads excerpts, calls Ollama, publishes results — framework-free to learn the raw consumer API |
| Results writer | **Spring Boot** + Spring Kafka + Spring Data JPA | Reads classified results, persists them to PostgreSQL |
| Frontend service | **Spring Boot** + Spring Kafka + Spring Web (`SseEmitter`) | Reads classified results, streams them live to the browser |
| Serialization | Jackson | Java objects <-> JSON |
| Kafka broker | Docker Compose (KRaft mode) | Local Kafka, no Zookeeper |
| Database | PostgreSQL (Docker) | Stores classified results |
| Local LLM | Ollama + `qwen2.5:7b` | Emotional-tone classification |
| Orchestration | Docker Compose | Brings up Kafka, Postgres, Ollama and all services with one command |

## Book Configuration

The producer reads `data/book-config.json` and the text file it points to:

```json
{
  "title": "The Metamorphosis",
  "file": "metamorphosis.txt",
  "startMarker": "*** START OF THE PROJECT GUTENBERG EBOOK",
  "endMarker": "*** END OF THE PROJECT GUTENBERG EBOOK",
  "sectionPattern": "^(I|II|III)$",
  "chunking": "paragraph",
  "minLength": 40
}
```

At startup the producer:

1. Drops everything before `startMarker` and after `endMarker` (the Gutenberg header and license).
2. Splits the rest into paragraphs (blank-line separated) and discards the ones shorter than `minLength`.
3. Sets each excerpt's `part` to the last line matching `sectionPattern` (`null` if the book has no sections).
4. Assigns sequential ids (`q-0001`, `q-0002`, ...).

## LLM Integration

**Endpoint:** `POST http://ollama:11434/api/generate` (Ollama's API, reached by service name inside the Docker network)

**Request (example):**

```json
{
  "model": "qwen2.5:7b",
  "prompt": "Classify the emotional tone of this excerpt in exactly one word from this list: anxiety, absurdity, resignation, alienation, bureaucracy, despair, confusion. Respond with only the word.\n\nExcerpt: \"<excerpt text>\"",
  "stream": false
}
```

**Response (example):**

```json
{
  "model": "qwen2.5:7b",
  "response": "alienation",
  "done": true
}
```

**Emotion categories** (a Java `enum Emotion` in the `common` module, stored as text in the database):

- anxiety
- absurdity
- resignation
- alienation
- bureaucracy
- despair
- confusion
- unknown — fallback, never sent in the prompt

**Handling the response:** the classifier extracts the `response` field, then normalizes it (trim, lowercase, strip punctuation, keep the first word). If the result isn't one of the seven categories, the emotion is `unknown`. If the Ollama call fails (timeout or HTTP error), the classifier retries 3 times and then also falls back to `unknown`, so one bad excerpt never blocks the topic.

The classification is attached to the original excerpt before publishing to `emotions`.

## Kafka Setup

| Topic | Partitions | Key | Notes |
| --- | --- | --- | --- |
| `quotes` | 3 | `part` | Excerpts of the same part keep their order |
| `emotions` | 3 | `part` | `retention.ms=-1` so the frontend can replay it from the start |

Topics are created by the `kafka-init` container in Docker Compose.

| Consumer group | Service | Reads |
| --- | --- | --- |
| `classifier` | classifier-consumer | `quotes` |
| `results-writer` | results-writer-service | `emotions` |
| `frontend` | frontend-service | `emotions` |

- **Partitioning:** a book has few parts (3 for *The Metamorphosis*), so there are only 3 distinct keys. Kafka hashes the key to pick the partition, so two parts may land on the same partition and one partition may stay empty.
- **Scaling the classifier:** up to 3 instances can share the `classifier` group, one per partition.
- **Slow LLM:** the classifier uses `max.poll.records=1`, so it takes one excerpt, waits for Ollama and only then polls again. This keeps it well below the default `max.poll.interval.ms` (5 minutes), after which Kafka would consider it dead and remove it from the group.
- **Lag:** if classification is slower than the producer, check how far behind the classifier is with:
  `docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092 --describe --group classifier`

## Data Format

**Topic 1:** `quotes`

**`Quote` model (Java `record`):**

```java
public record Quote(
    String id,
    String text,
    String book,
    String part   // nullable
) {}
```

**Message published on topic 1 (JSON):**

```json
{
  "id": "q-0042",
  "text": "<book excerpt>",
  "book": "The Metamorphosis",
  "part": "I"
}
```

**Topic 2:** `emotions`

**`EmotionResult` model (Java `record`):**

```java
public record EmotionResult(
    String quoteId,
    String text,
    Emotion emotion,
    String book,
    String part,   // nullable
    Instant classifiedAt
) {}
```

**Message published on topic 2 (JSON):**

```json
{
  "quoteId": "q-0042",
  "text": "<book excerpt>",
  "emotion": "alienation",
  "book": "The Metamorphosis",
  "part": "I",
  "classifiedAt": "2026-10-03T14:22:10Z"
}
```

**Database schema (PostgreSQL, created from `schema.sql` in the results-writer):**

```sql
CREATE TABLE IF NOT EXISTS classified_quotes (
  id            SERIAL PRIMARY KEY,
  quote_id      TEXT NOT NULL,
  book          TEXT NOT NULL,
  part          TEXT,
  text          TEXT NOT NULL,
  emotion       TEXT NOT NULL,
  classified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (book, quote_id)
);
```

Kafka delivers messages at least once, and restarting the producer republishes the whole book, so the same result can arrive twice. The writer inserts with `ON CONFLICT DO NOTHING`, and the `UNIQUE (book, quote_id)` constraint makes duplicates harmless.

The table keeps the history for SQL queries (e.g. dominant emotion per part of the book), browsable through Adminer. The writer maps each `EmotionResult` to a `ClassifiedQuoteEntity` (the JPA class for this table).

## Real-Time Frontend

A Spring Boot service that reads only from Kafka, never from the database:

- On startup, consumes `emotions` **from the beginning** (replay) and rebuilds in memory the running count of each emotion and the latest classifications. It doesn't rely on its committed offsets for this.
- Exposes `GET /stats` with that in-memory state, and `/events` backed by Spring Web's `SseEmitter`, which pushes every new classification to connected browsers.
- Serves a static page (`index.html`) with plain JavaScript: on load it calls `/stats` to draw the current state, then listens via `EventSource` and updates the screen as new classifications arrive. Reloading the page doesn't reset the counts.

## Project Structure

```
kafka-on-kafka/
├── pom.xml
├── docker-compose.yml
├── data/
│   ├── book-config.json
│   └── metamorphosis.txt
├── common/
│   └── src/main/java/.../{Quote,EmotionResult,Emotion}.java
├── producer/
│   └── src/main/java/.../QuoteProducer.java
├── classifier-consumer/
│   ├── src/main/java/.../EmotionClassifierConsumer.java
│   └── src/main/java/.../OllamaClient.java
├── results-writer-service/
│   ├── src/main/java/.../ResultsWriterApplication.java
│   ├── src/main/java/.../ClassifiedQuoteEntity.java
│   └── src/main/resources/{application.yml,schema.sql}
└── frontend-service/
    ├── src/main/java/.../FrontendApplication.java
    ├── src/main/java/.../SseController.java
    ├── src/main/resources/application.yml
    └── src/main/resources/static/index.html
```

Each service is its own Maven module with its own `Dockerfile` and depends on `common`; all of them are orchestrated by `docker-compose.yml`.

## Docker Compose — Services

Everything runs inside Docker, so services reach each other by name (`kafka:9092`, `ollama:11434`, `postgres:5432`), configured through environment variables.

| Service | Image/Build | Port (host) |
| --- | --- | --- |
| kafka | `apache/kafka` (KRaft mode), internal listener `kafka:9092`, external listener for host tools | 29092 |
| kafka-init | `apache/kafka`, creates the topics and exits | — |
| postgres | `postgres:16` | 5432 |
| adminer | `adminer`, web UI to browse the database | 8081 |
| ollama | `ollama/ollama` | 11434 |
| ollama-init | `ollama/ollama`, pulls `qwen2.5:7b` and exits | — |
| producer | local build | — |
| classifier-consumer | local build, starts after `ollama-init` finishes | — |
| results-writer-service | local build (Spring Boot) | — |
| frontend-service | local build (Spring Boot) | 8080 |

The producer's delay between excerpts is set with `PRODUCER_DELAY_MS` (default `3000`). A short delay (e.g. `500`) makes the classifier fall behind, which is a good way to watch lag grow; a delay longer than the model's response time keeps lag near zero.

## Running

```
docker compose up --build
```

The first run downloads the model, which takes a while. Then open http://localhost:8080 for the live view and http://localhost:8081 (Adminer; system `PostgreSQL`, server `postgres`) to browse the database.

Example queries on `classified_quotes`:

```sql
-- emotions per part of the book
SELECT part, emotion, count(*) FROM classified_quotes GROUP BY part, emotion ORDER BY part, count(*) DESC;

-- dominant emotion per part
SELECT DISTINCT ON (part) part, emotion, count(*)
FROM classified_quotes GROUP BY part, emotion ORDER BY part, count(*) DESC;

-- excerpts the model couldn't classify
SELECT quote_id, text FROM classified_quotes WHERE emotion = 'unknown';
```

## Design Decisions

- Database: PostgreSQL, with the emotion stored as text and the allowed values enforced by the Java `Emotion` enum
- Emotion categories: 7 fixed ones plus `unknown` as fallback
- Excerpt granularity: by paragraph, configured in `book-config.json`
- Two Kafka topics, decoupling classification from consuming the results
- Framework split: producer and classifier consumer use plain `kafka-clients`; results-writer and frontend use Spring Boot (Spring Kafka, Spring Data JPA, Spring Web)
- Frontend: real time via Server-Sent Events, state rebuilt by replaying the `emotions` topic
- Everything orchestrated via Docker Compose, including Ollama
