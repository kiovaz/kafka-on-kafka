> **Status:** code, unit tests (47 of 47 passed) and docs are done and committed. Items ticked in section 5 were seen in a real run against Kafka, Ollama and Neon; the open ones are run in order by `docs/RUNBOOK.md`.

## 1. State

- [x] 1.1 Add `LiveFeed` and the `Stats` record in `application` (map keyed by book and quote id, first result wins, counts for all eight emotions with zeros, latest 20 by `classifiedAt` newest first, subscribers called only for new results, unsubscribe) and `LiveFeedTest` covering the `live-feed` scenarios, and verify the tests pass

## 2. Listener

- [x] 2.1 Add `FeedListener` in `adapter.in.kafka` (group `frontend`, default value type `EmotionResult` on the listener, `ConsumerSeekAware` seeking to the beginning on every assignment, gives each result to `LiveFeed`) and `FeedListenerTest` with a mocked seek callback, and verify the tests pass

## 3. Web

- [x] 3.1 Add `FeedController` in `adapter.in.web` (`GET /stats` returning `Stats`, `GET /events` with an `SseEmitter` without timeout that subscribes to `LiveFeed` and is removed on completion, timeout or error) and `FeedControllerTest` with MockMvc in standalone mode for the `/stats` JSON shape, and verify the tests pass
- [x] 3.2 Add `src/main/resources/static/index.html` (count per emotion, latest results, status of the live connection, opens `EventSource` first then fetches `/stats`, merges by quote id) and verify `mvn test` passes

## 4. Documentation

- [x] 4.1 Update the README (the live feed and its two endpoints, the replay and its limit) and add decision 23 to `docs/DECISIONS.md` (state keyed by quote, replay by seeking to the beginning), and run `openspec validate add-live-feed`

## 5. Final checks (run at the end of the whole development; need Docker, Kafka and the app)

- [x] 5.1 Open `http://localhost:8080` while the book is being classified and verify the counts and the list update without a reload — seen: /stats equal to the database (97), events 1 to 97 in order
- [ ] 5.2 Reload the page and verify the counts are the same as before
- [ ] 5.3 Restart the app and verify `/stats` shows all results again after a few seconds (replay)
- [ ] 5.4 Republish the book (restart with the publisher on) and verify the counts do not grow
- [ ] 5.5 Open two browser tabs and verify both update, then close one and verify the other keeps updating
- [ ] 5.6 Start a second instance of the app and verify both pages stay correct after the partitions are shared (rebalance replay)
