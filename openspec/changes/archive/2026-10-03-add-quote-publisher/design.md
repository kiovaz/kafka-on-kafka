## Context

The app builds and starts against the Docker infrastructure; the `domain` package has `Quote`. The README and `docs/DECISIONS.md` fix the rules: hexagonal package layout, classes calling each other directly (no interfaces), Spring Kafka, topic `quotes` with 3 partitions and key = `part`, the book read at startup from `data/`. See proposal.md for scope.

Facts checked: the text file uses LF line endings, paragraphs are separated by blank lines and lines wrap at about 70 characters, and only blank lines sit between the start marker and the first section line `I`. Spring Kafka 4.1.1 provides `JacksonJsonSerializer` (Jackson 3) next to the older `JsonSerializer`.

## Goals / Non-Goals

**Goals:**
- Quotes flow from the book file to the `quotes` topic at a steady pace, in order, without skipping on failure.
- Parsing and publishing logic are covered by fast tests that need no Kafka.

**Non-Goals:**
- Consuming anything (blocks 4-6), Docker packaging of `data/` (block 7), Kafka transactions or exactly-once publishing.
- An integration test with a real broker: the check against the Docker Kafka is done by hand and recorded in the tasks.
- Supporting other chunking modes than `paragraph`.

## Decisions

**Four small classes, one per role, no interfaces:**
- `adapter.out.book.BookReader` (with a private `BookConfig` record): file and parsing details.
- `adapter.out.kafka.QuotePublisher`: the only class that knows `KafkaTemplate`.
- `application.PublishBook`: the logic "next quote, publish, advance".
- `adapter.in.scheduler.PublishBookJob`: the timer, which only calls `PublishBook`.

The use case is separate from the job so it can be tested by calling it directly with a mocked `QuotePublisher` (Mockito can mock a class), without waiting for a scheduler.

**Scheduling with `@Scheduled(fixedDelayString = "${app.publisher.delay-ms}")`.** `fixedDelay` counts from the end of the previous run, so the wait for Kafka's acknowledgement is included and runs never overlap. Rejected: a hand-written thread with `sleep` (more code, manual shutdown handling). `@EnableScheduling` sits on the job class.

**Synchronous send, waiting for the acknowledgement** (`send(...).get(timeout)`), with the producer's `max.block.ms` also set to 10 s: Kafka's default of 60 s would otherwise hold the thread for a minute before the timeout even starts when the broker is unreachable (found during the checks). `PublishBook` advances to the next quote only after it returns normally; on an exception it stays on the same quote, which the next tick retries. At 3 s per quote the blocking costs nothing, and it gives the "no skipped quote" rule with a single `if`. Rejected: fire-and-forget with a callback (a failed send would be skipped or need its own retry queue).

**The on/off switch is `@ConditionalOnProperty` on the job and on `PublishBook`** (`app.publisher.enabled`, default true, from `APP_PUBLISHER_ENABLED`). `PublishBook` reads the whole book in its constructor, so a broken config fails the startup immediately with a clear message, and with the switch off the book is never read.

**JSON through Spring Kafka's `JacksonJsonSerializer`, configured in `application.yml`** (`spring.kafka.producer.value-serializer`), with `spring.json.add.type.headers=false` so messages are plain JSON with no Java class name in a header (the consumers in later blocks should not need to know our class names). Rejected: serializing by hand to a `String` (extra code for what the property already does). If the property names turn out different in Spring Boot 4, the fallback is a `String` value serializer with a `JsonMapper`.

**Message key = `part`** (null part gives a null key, which Kafka spreads across partitions). Topic name `quotes` is a constant in `QuotePublisher`.

**Parsing, in `BookReader`:** read the file; take the lines strictly between the first line containing `startMarker` and the first line containing `endMarker`; walk the lines keeping the current section: a line matching `sectionPattern` (tested on the trimmed line) sets it; blank lines end a paragraph; other lines accumulate; each finished paragraph has its whitespace collapsed (`\s+` to a single space) and is kept when at least `minLength` long. `chunking` must be `paragraph`. The config path comes from `app.book.config-path` (`BOOK_CONFIG_PATH`, default `data/book-config.json`), and the text file is looked up next to the config.

**Tests:**
- `BookReaderTest` over a small fixture in `src/test/resources` (header, footer, wrapped lines, short paragraph, two sections, text before the first section) plus the error cases.
- A second test class (or method) reads the real `data/` book and checks parts `I`, `II`, `III` and that "Project Gutenberg" appears nowhere.
- `PublishBookTest` with Mockito: order, stops at the end, a failing publish retried on the next call.

**New properties** in `application.yml`: `app.publisher.enabled`, `app.publisher.delay-ms`, `app.book.config-path`, and the producer serializer settings.

## Risks / Trade-offs

- [Spring Kafka 4 / Boot 4 property names for the JSON serializer may differ] → Verify in the first step; fall back to a `String` serializer plus `JsonMapper`.
- [The real book may yield odd quotes (very long paragraphs, stray lines)] → The real-data test asserts structure, and the first/last quotes and the count are inspected during the check.
- [Restarting republishes the whole book, so `quotes` fills with duplicates] → Accepted by design (decision 10); the checks recreate the topic afterwards.
- [A blocked send holds the scheduler thread] → Only one task runs, so nothing else waits; both the metadata wait (`max.block.ms`) and the acknowledgement wait are 10 s.
- [`data/` is found through a path relative to the working directory] → Works from the project root and from IntelliJ; Docker will mount or copy it in block 7 and can override `BOOK_CONFIG_PATH`.
