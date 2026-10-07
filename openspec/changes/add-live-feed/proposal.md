## Why

The results should be visible as they are produced. The live feed is the third consumer group on `emotions` (`frontend`): it keeps the current state in memory and shows it in a web page that updates by itself. It reads only from Kafka, so reloading the page or restarting the app rebuilds everything by replaying the topic. See `docs/DECISIONS.md` (6, 9).

## What Changes

- Add `LiveFeed` (`application`): keeps one result per quote in memory, computes the counts and the latest results, and tells subscribers about each new result.
- Add `FeedListener` (`adapter.in.kafka`): group `frontend` on `emotions`, going back to the first message every time partitions are assigned, and giving each result to `LiveFeed`.
- Add `FeedController` (`adapter.in.web`): `GET /stats` and `GET /events` (Server-Sent Events).
- Add a static page `src/main/resources/static/index.html` (plain HTML, CSS and JavaScript) showing the count per emotion and the latest results, updated live.
- Tests: `LiveFeed`, the listener's seek, and the `/stats` response.
- Update the README and `docs/DECISIONS.md`.
- Nothing is run in this change; the end-to-end checks are the last tasks and are done at the end of the whole development.

## Capabilities

### New Capabilities
- `live-feed`: the in-memory state rebuilt from the topic, the two endpoints and the page.

### Modified Capabilities

## Impact

- New: `LiveFeed`, `Stats`, `FeedListener`, `FeedController`, `static/index.html`, tests. No new dependency (the web starter is already there).
- The page is served by the same app on port 8080.
- A restart replays the whole `emotions` topic (it keeps its messages forever), so the feed is correct again within moments.
