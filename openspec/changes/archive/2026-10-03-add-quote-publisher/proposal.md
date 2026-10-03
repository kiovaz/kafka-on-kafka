## Why

The app builds and starts, but nothing flows yet. The publisher is the start of the pipeline: it turns the book into excerpts and sends them to Kafka one at a time, which every later block (classifier, writer, live feed) consumes. See `docs/DECISIONS.md` (5, 7).

## What Changes

- Add `BookReader` (`adapter.out.book`): reads `data/book-config.json` and the text file it points to, and returns the book as a list of `Quote`.
- Add `QuotePublisher` (`adapter.out.kafka`): sends a `Quote` to the `quotes` topic as JSON, with the quote's `part` as the message key, and waits for Kafka's acknowledgement.
- Add `PublishBook` (`application`): holds the quotes and, each time it is called, publishes the next one.
- Add `PublishBookJob` (`adapter.in.scheduler`): calls `PublishBook` every `PRODUCER_DELAY_MS` (default 3000) after startup.
- Add configuration for the Kafka JSON serializer, the delay, the on/off switch and the config file path.
- Tests: the reader (small fixture and the real book) and the publishing logic (with Mockito).
- Update the README and `docs/DECISIONS.md`.
- No new dependencies, no interfaces.

## Capabilities

### New Capabilities
- `book-reading`: how the book text becomes a list of quotes, and when reading fails.
- `quote-publishing`: how and when the quotes reach the `quotes` topic.

### Modified Capabilities

## Impact

- New classes under `src/main/java/com/kiovaz/kafkaonkafka/{application,adapter}`, new properties in `application.yml`, new tests and a small test fixture.
- The app now writes to Kafka when it starts (with `docker compose up -d` running). Restarting it publishes the whole book again; consumers are expected to cope with duplicates (the writer does, see decision 10).
- Messages stay in `quotes` (Kafka data is persistent), so the topic must be recreated after manual checks.
