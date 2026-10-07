## Purpose

Defines the live view of the classifications: the state kept in memory from the `emotions` topic, the two HTTP endpoints and the web page.

## ADDED Requirements

### Requirement: State rebuilt from the topic
The application SHALL read the topic `emotions` as the consumer group `frontend` and, every time it starts or is given partitions, SHALL go back to the first message and rebuild its state from there. It SHALL NOT read the database.

#### Scenario: Restart
- **WHEN** the application is restarted after results were published
- **THEN** its state again contains all the results in the topic

### Requirement: One entry per quote
The state SHALL hold at most one result per (`book`, `quote_id`). A result that arrives again for a quote already known (a republished book, or a replay after a rebalance) SHALL be ignored and the first result kept.

#### Scenario: Same quote twice
- **WHEN** two results for the same book and quote id arrive
- **THEN** the quote is counted once, with the first result

### Requirement: Stats endpoint
`GET /stats` SHALL return JSON with `seq` (the number of the last event included in the snapshot), `total` (the number of quotes known), `counts` (the number of quotes for each of the eight emotions, keyed by the lowercase name, including those with zero) and `latest` (the up to 20 most recently classified results, newest first, each as the `EmotionResult` JSON).

#### Scenario: Counts include every emotion
- **WHEN** only `despair` results are known
- **THEN** `counts` has all eight emotions, `despair` with the right number and the others with zero

#### Scenario: Latest results
- **WHEN** more than 20 results are known
- **THEN** `latest` has exactly 20, the most recently classified first

#### Scenario: Empty state
- **WHEN** no result is known
- **THEN** `seq` and `total` are 0, every count is 0 and `latest` is empty

### Requirement: Events endpoint
`GET /events` SHALL be a Server-Sent Events stream that sends each result added to the state, as the `EmotionResult` JSON, at the moment it is added, with the event id set to a sequence number that grows by one for each result added. A result ignored as a duplicate SHALL NOT be sent.

#### Scenario: New result
- **WHEN** a client is connected and a result for a new quote arrives
- **THEN** the client receives an event with that result

#### Scenario: Duplicate result
- **WHEN** a result for an already known quote arrives
- **THEN** no event is sent

#### Scenario: Numbered events
- **WHEN** two new results are added one after the other
- **THEN** their event ids differ by one, and `/stats` taken afterwards reports the id of the last one as `seq`

#### Scenario: Client leaves
- **WHEN** a client disconnects
- **THEN** it is no longer sent events and the others are not affected

### Requirement: Page
`GET /` SHALL serve a page that shows the count for each emotion and the latest results, loads the current state from `/stats` and then keeps it up to date from `/events` without a reload. The page SHALL not count a quote twice: it SHALL ignore every event whose id is not greater than the `seq` of the snapshot it loaded, and it SHALL load the snapshot again whenever the stream reconnects.

#### Scenario: Reload
- **WHEN** the page is reloaded
- **THEN** it shows the same counts as before the reload

#### Scenario: Live update
- **WHEN** a new result is classified while the page is open
- **THEN** the page updates its counts and its list without a reload
