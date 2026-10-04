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

**Decision:** the classifier cleans the model's answer; anything outside the 7 emotions becomes `unknown`. If the Ollama call fails it retries 3 times; if it still fails for a reason that can fix itself (unreachable, timeout, server error, unreadable answer) the same quote is retried later instead of being recorded, and only a client error (4xx) gives `unknown` (see decision 18). The classifier listener uses `max.poll.records=1`.

**Why:** a consumer that fails forever on one message never advances its offset, so everything behind it waits. One message per poll also keeps a slow model below `max.poll.interval.ms` (5 minutes).

## 9. The live feed reads only from Kafka and replays the topic

**Decision:** on startup the live feed reads `emotions` from the beginning and rebuilds the counts and the latest results in memory. `GET /stats` returns that state; `/events` (Server-Sent Events) pushes new results. The page is a static file in `src/main/resources/static/`; the endpoints live in `adapter.in.web`.

**Why:** reloading the page must not reset the counts, and the frontend should not depend on the database. Replay is one of the Kafka features the project exists to show.

**Rejected:** having the frontend query PostgreSQL for the initial state.

## 10. PostgreSQL stores the history, browsable in the Neon console

**Decision:** the results writer saves each result to `classified_quotes` with `UNIQUE (book, quote_id)` and `ON CONFLICT DO NOTHING`. The emotion is stored as text. The data is browsed in the Neon console (Tables and SQL Editor); Adminer was dropped when the database moved to Neon (decision 19).

**Why:** Kafka delivers at least once and a restart republishes the book, so duplicates must be harmless. A database nobody can look at has no value. The Java `Emotion` enum already enforces the allowed values, so a PostgreSQL ENUM type would only complicate JPA.

**Rejected:** a PostgreSQL ENUM, and the frontend reading from this table.

## 11. Everything runs in Docker

**Decision:** Docker Compose runs Kafka (KRaft), Ollama and the app (the database is hosted, see decision 19). Services reach each other by name (`kafka:9092`, `ollama:11434`). Kafka has an internal listener (`kafka:9092`) and an external one (`localhost:29092`) for host tools. Kafka and Ollama data live in named volumes. Image versions are pinned (`apache/kafka:4.3.1`, `ollama/ollama:0.35.1`). One-shot containers create the topics (`kafka-init`) and pull the model (`ollama-init`).

**Why:** one command brings everything up, reproducibly. Inside a container `localhost` is the container itself, so service names are required. Volumes keep topics and messages across `docker compose down`.

## 12. Specs and decisions are versioned; the AI tool's files are not

**Decision:** `openspec/` (main specs and the archive of finished changes) is committed. `.claude/`, `.idea/` and `TODO.md` stay local and ignored.

**Why:** the specs and their design notes record why things were built; the others are personal tooling.

**Note:** specs and the README describe some of the same things. The README is the overview; the specs are the contract for each part.

## 13. The publisher waits for Kafka's acknowledgement and retries the same quote

**Decision:** `QuotePublisher` sends a quote and waits for Kafka to acknowledge it (10 s timeout; the producer's `max.block.ms` is also set to 10 s). `PublishBook` moves to the next quote only after that, so a failed publish is retried on the next turn instead of skipping the quote.

**Why:** one quote every 3 seconds makes waiting free, and it gives "no quote is skipped" with a single rule. Without `max.block.ms`, Kafka's default would hold the thread for 60 s before the 10 s timeout even starts when the broker is unreachable (seen when the address was wrong).

**Rejected:** fire-and-forget with a callback, where a failed send would be lost or need its own retry queue.

## 14. A scheduler drives the publisher; the logic is a separate class

**Decision:** `PublishBookJob` (`adapter.in.scheduler`, `@Scheduled` with `fixedDelay` from `PRODUCER_DELAY_MS`) only calls `PublishBook.publishNext()` (`application`). `APP_PUBLISHER_ENABLED=false` removes both beans, so the book is not even read.

**Why:** `fixedDelay` counts from the end of the previous run, so runs never overlap and the acknowledgement wait is included. Keeping the logic in its own class lets a test call it directly with a mocked `QuotePublisher`.

**Rejected:** a hand-written thread with `sleep`.

## 15. Messages are plain JSON, with no Java type header

**Decision:** values are written by Spring Kafka's `JacksonJsonSerializer` (Jackson 3) with `spring.json.add.type.headers=false`. Verified: the messages have no headers.

**Why:** consumers (later blocks) should not need to know our class names.

**Observed:** the key hashing sent parts `I` and `III` to partition 2, `II` to partition 1, and left partition 0 empty (see decision 5).

## 16. Ollama runs in Docker on the GPU, with a small model

**Decision:** the `ollama` service (pinned `0.35.1`) reserves the NVIDIA GPU and sets `OLLAMA_KEEP_ALIVE=-1`; the one-shot `ollama-init` downloads the model and loads it once. The default model is `qwen2.5:3b` (about 1.9 GB, 2.2 GB of VRAM), configurable with `OLLAMA_MODEL`.

**Why:** the machine has 7.7 GB of RAM, Docker Desktop is limited to 3.9 GB, and there is a 6 GB RTX 4050 that Docker can see. The first attempt with `qwen2.5:7b` (4.7 GB) made the machine freeze, so a lighter model was chosen. On the GPU a warm call takes about 0.15 s. The first call after a cold start took 149 s, so loading it in `ollama-init` and keeping it loaded moves that wait out of the application.

**Rejected:** the 7B model (too heavy here), Ollama on CPU in Docker (does not fit the Docker memory), and Ollama installed natively on Windows (breaks "everything runs in Docker", decision 11).

## 17. The classifier reads from the beginning and never loses a result

**Decision:** the `classifier` group uses `auto-offset-reset: earliest`, one message per poll, and values go through `ErrorHandlingDeserializer` wrapping `JacksonJsonDeserializer` (no type headers; the default type is set per listener). A `DefaultErrorHandler` with a 2 s fixed back-off and no attempt limit retries the same message when the listener throws, which only happens when publishing the result fails. A message that cannot be read is logged once (`DeserializationException`) and skipped.

**Why:** a new group must read what the publisher already wrote; a result lost in a short Kafka hiccup is worse than a short wait; one unreadable message must not stall the partition.

**Rejected:** Spring's default error handling (10 immediate attempts, then skip), which could drop a result.

## 18. An Ollama outage makes the classifier wait, not guess

**Decision:** after 3 failed attempts, if the cause can fix itself (Ollama unreachable, a timeout, a 5xx, an unreadable answer), `EmotionClassifier` throws and the listener's error handler retries the same message every 2 s without limit. No result is recorded for the outage. Only a 4xx (the request itself is wrong, retrying cannot help) or an answer outside the emotion set gives `unknown`.

**Why:** the first version (decision 8 as written: `unknown` after any 3 failures) was tested with Ollama stopped for about a minute and recorded 34 permanent wrong `unknown` results. The new rule was tested the same way: all 97 quotes were classified exactly once, none skipped, none wrong, and the recovery took 286 s including the model reload.

**Cost:** during an outage the topic stalls and lag builds up; a quote that Ollama keeps failing on with a 5xx would stall its partition (visible in the log).

**Also observed:** with `qwen2.5:3b` most excerpts are classified as `alienation` (55 of 97); the model, not the pipeline, is the limit.

## 19. The database is Neon, not a container

**Decision:** the local `postgres` and `adminer` containers (and the `pgdata` volume) were removed. The app connects to a hosted PostgreSQL on Neon over TLS (`sslmode=require`, `channelBinding=require`). The connection (`DB_URL`, `DB_USER`, `DB_PASSWORD`) has no default in `application.yml`; it comes from the environment or from a git-ignored `.env` file that the app imports on startup (`spring.config.import`). `.env.example` is versioned with placeholders; the real password is never committed.

**Why:** one container less to run on a machine short of memory, and no local database to start. The connection was tested: the app starts, opens the pool and the password does not appear in the log.

**Honest note:** the local Postgres used only about 36 MB (Adminer about 15 MB), so this saves little. The real memory use is Ollama (about 2 GB of RAM), Kafka (about 0.3 GB) and the IDE, with Windows left at about 0.1 GB free of 7.7 GB.

**Rejected:** putting the real values in `application.yml` or `application.properties` (the repository is public), and reading `config/application.properties` instead of `.env` (works the same, but `.env` also feeds Docker Compose when the app moves into a container in block 7).

**Risk:** the password was pasted into a chat to set this up; it should be reset in the Neon console and `.env` updated.

## 20. The classifier asks with temperature 0

**Decision:** the request to Ollama sets `options.temperature` to 0.

**Why:** with Ollama's default temperature, running the same prompt twice on the same 97 excerpts changed the emotion of 31 of them (32%); only 43 of 97 kept the same emotion across four prompt variants. At 0 the model always picks its most likely answer, so the result for an excerpt is repeatable (a restart that republishes the book gets the same labels).

**Observed, not fixed yet:** the 3B model still leans on `alienation` (about 55% of the excerpts) and never answers `bureaucracy`, even for the chief-clerk passages. Adding the category definitions did not change that. Adding seven example passages moved the bias instead of removing it (`confusion` took 57 of 97, probably because the last example was a `confusion` one: recency bias), so any examples must be balanced and shuffled. These checks were done with a script outside the repository, not part of the build. A larger model is the likely fix but the 7B one froze the machine.

**Rejected:** a fixed seed (not needed at temperature 0).
