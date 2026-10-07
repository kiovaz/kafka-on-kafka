## Context

Results are in `emotions` (kept forever, key = `part`). The global consumer settings already deserialize JSON without type headers and skip unreadable messages; the default value type is set per listener (the writer will set its own). `spring-boot-starter-webmvc` is already a dependency. Spring Kafka 4.1.1 has `ConsumerSeekAware.onPartitionsAssigned(Map<TopicPartition, Long>, ConsumerSeekCallback)` and `ConsumerSeekCallback.seekToBeginning(Collection<TopicPartition>)`. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- A page that shows the counts and the latest results and updates by itself, and that survives a reload and a restart.
- State that stays correct under duplicates and replays.

**Non-Goals:**
- Reading the database, authentication, history beyond the 20 latest, charts or any front-end framework or build step.
- Running anything in this change.

## Decisions

**State is a map keyed by (`book`, `quote_id`), and everything is computed from it.** Counts and the latest list are derived from the map's values on each `/stats` call (at most one entry per quote, so at most a few hundred entries). This is what makes replays harmless: reading the topic from the start again after a rebalance, or a republished book, only tries to add keys that already exist. First result wins, like the database (decision 10). Rejected: incremental counters, which double count on every replay.

**Replay by seeking to the beginning on every assignment.** `FeedListener` implements `ConsumerSeekAware` and calls `callback.seekToBeginning(assignments.keySet())` in `onPartitionsAssigned`, so the saved offsets of the `frontend` group are never relied on. Rejected: a fresh group id per start (leaves a trail of dead groups) and reading the database for the initial state (decision 9).

**Events are numbered, and adding and reading are synchronized.** `LiveFeed` gives each new result a sequence number (`FeedEvent(id, result)`) and `/stats` reports the last one as `seq`. The page opens the stream, loads `/stats`, and then ignores every event with an id not greater than that `seq`: this is what keeps a result that arrives between the two from being counted twice, which merging by quote id alone could not guarantee for the counts. `record` and `stats` are `synchronized` (one writer thread, rare readers) so a snapshot and its `seq` always agree. The page also reloads the snapshot whenever the stream reconnects, because events sent while it was disconnected are not replayed.

**Notification by plain callbacks.** `LiveFeed.subscribe(Consumer<FeedEvent>)` returns a way to unsubscribe, and `record` calls the subscribers only when the result was new. The web adapter registers one subscriber per SSE client. Rejected: Spring application events or a message broker for a list of lambdas.

**Four small classes plus the page, no interfaces:** `application.LiveFeed` (+ `Stats` record), `adapter.in.kafka.FeedListener`, `adapter.in.web.FeedController`, `static/index.html`.

**`/stats` keys are plain strings** (`emotion.json()`), built in enum order with zeros included, so the JSON does not depend on how a serializer writes enum map keys.

**SSE with `SseEmitter` and no timeout.** `/events` creates an emitter, subscribes it, and removes it on completion, timeout or error (a failed send also removes it). Browsers reconnect by themselves.

**The page opens the event stream first and then fetches `/stats`** (buffering events until the snapshot arrives), applies the snapshot as the base for the counts and the list, then applies buffered and later events with an id greater than `seq`. Plain JavaScript, no libraries, no build.

**Per-listener setup:** `@KafkaListener(topics = "emotions", groupId = "frontend", properties = "spring.json.value.default.type=com.kiovaz.kafkaonkafka.domain.EmotionResult")`. The writer will use the same topic with its own group.

**Tests without Docker:** `LiveFeedTest` (duplicates ignored and first kept, counts with zeros, latest order and limit, subscribers called only for new results, unsubscribe), `FeedListenerTest` (seeks to the beginning with a mocked callback) and `FeedControllerTest` (MockMvc in standalone mode: `/stats` JSON shape with lowercase keys).

## Risks / Trade-offs

- [Replaying the whole topic on each start grows with the topic] → Fine for a few hundred messages; noted in the README as a limit.
- [A rebalance replays the topic, producing a burst of work] → Duplicates are ignored in memory, so only cost, not correctness, is affected.
- [`SseEmitter` sends from the Kafka listener thread, so a slow browser could slow the consumer] → Sends are small and a failing client is dropped; acceptable at this scale, noted as a limit.
- [The SSE stream and a reverse proxy buffering] → There is no proxy in this setup.
- [The seek and the SSE behaviour cannot be unit tested end to end] → Covered by the final checks.
