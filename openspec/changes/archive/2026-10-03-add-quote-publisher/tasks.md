## 1. Configuration

- [x] 1.1 Add `app.publisher.enabled`, `app.publisher.delay-ms`, `app.book.config-path` and the Kafka producer JSON serializer settings (no type headers) to `application.yml`, and verify the app still starts with `mvn spring-boot:run` and the serializer class resolves (fall back to a `String` serializer + `JsonMapper` if the property names differ)

## 2. Book reading

- [x] 2.1 Add `BookReader` and its `BookConfig` record in `adapter.out.book` (markers, paragraphs, whitespace, `minLength`, sections, ids, fail-fast errors) and verify it compiles
- [x] 2.2 Add a small fixture in `src/test/resources` and `BookReaderTest` covering the `book-reading` scenarios (header/footer, wrapped lines, short paragraph, sections, null part before the first section, sequential ids, missing marker, unsupported chunking), and verify the tests pass
- [x] 2.3 Add a test over the real `data/` book (parts `I`, `II`, `III` present, no "Project Gutenberg" in any quote), verify it passes, and note the number of quotes and the first and last quote text

## 3. Publishing

- [x] 3.1 Add `QuotePublisher` in `adapter.out.kafka` (topic `quotes`, key = `part`, waits for the acknowledgement with a 10 s timeout) and verify it compiles
- [x] 3.2 Add `PublishBook` in `application` (reads the book in the constructor, `publishNext()` advances only after a successful publish, does nothing after the last quote) and `PublishBookTest` with Mockito (order, end of book, retry after a failure), and verify the tests pass
- [x] 3.3 Add `PublishBookJob` in `adapter.in.scheduler` (`@Scheduled` with the configured delay, both beans conditional on `app.publisher.enabled`) and verify `mvn verify` passes

## 4. Checks against the real infrastructure

- [x] 4.1 With `docker compose up -d` running, start the app with `PRODUCER_DELAY_MS=200` and verify with `kafka-console-consumer` (key, partition and timestamp printed) that messages arrive in order as JSON with the right fields, that quotes of the same part share a partition, and that the gaps between timestamps are about 200 ms or more
- [x] 4.2 Verify the Kafka producer log shows `bootstrap.servers` with the default address, then start with `KAFKA_BOOTSTRAP_SERVERS` set to a wrong address and verify the log shows that address and the publish fails after 10 s with nothing published (this also settles the override left open in `app-config`)
- [x] 4.3 Start with `APP_PUBLISHER_ENABLED=false` and verify nothing is published and the book is not read
- [x] 4.4 Restart the app and verify it publishes again from `q-0001` (the topic's message count grows by the book's size)
- [x] 4.5 Delete and recreate the `quotes` topic (`kafka-init`) so it is clean, and verify its offsets are 0

## 5. Documentation and wrap-up

- [x] 5.1 Update the README (the startup trigger now lives in `adapter.in.scheduler`, the new variables `PRODUCER_DELAY_MS`, `APP_PUBLISHER_ENABLED`, `BOOK_CONFIG_PATH`) and add the new decisions (synchronous send with acknowledgement, scheduler, JSON serializer without type headers) to `docs/DECISIONS.md`
- [x] 5.2 Verify `git status` shows no `target/` output, then run `openspec validate add-quote-publisher`
