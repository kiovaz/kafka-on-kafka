## Purpose

Defines how classified results are stored in PostgreSQL: the table, the consumer group that fills it, and how duplicates, a missing database and bad messages are treated.

## ADDED Requirements

### Requirement: Table created at startup
At startup the application SHALL create the table `classified_quotes` when it does not exist, with the columns `id` (generated), `quote_id`, `book`, `part` (nullable), `text`, `emotion` (text), `classified_at` (timestamp with time zone) and a uniqueness rule on (`book`, `quote_id`). Starting again SHALL NOT fail or change existing rows.

#### Scenario: Empty database
- **WHEN** the application starts against a database without the table
- **THEN** the table exists afterwards

#### Scenario: Second start
- **WHEN** the application starts again with rows already in the table
- **THEN** it starts normally and the rows are unchanged

### Requirement: One row per result
The application SHALL read the topic `emotions` as the consumer group `results-writer`, from the first message when the group has no saved position, and SHALL insert one row per result with `quote_id`, `book`, `part`, `text`, `emotion` as its lowercase name and `classified_at` from the result.

#### Scenario: New result
- **WHEN** a result for quote `q-0042` with emotion despair is published to `emotions`
- **THEN** a row exists with `quote_id` `q-0042` and `emotion` `despair`

#### Scenario: Result without a part
- **WHEN** a result has no `part`
- **THEN** its row has a null `part`

### Requirement: Duplicates are harmless
When a result for the same (`book`, `quote_id`) arrives again (the book is republished on every start, and Kafka may deliver a message twice), the application SHALL keep the first row and ignore the new one without error.

#### Scenario: Same result twice
- **WHEN** two results for the same book and quote id arrive
- **THEN** the table has exactly one row for it, the one that arrived first

#### Scenario: Restart
- **WHEN** the application is restarted and the whole book is classified again
- **THEN** the table has no more rows than before

### Requirement: No result is lost when the database is unavailable
If an insert fails, the application SHALL try the same message again (waiting between attempts, without limit) instead of skipping it, and the following results SHALL wait behind it.

#### Scenario: Database down then up
- **WHEN** the database is unreachable while a result is being written and becomes reachable later
- **THEN** that result is written and the results after it follow, with none missing

### Requirement: A bad message does not block
A message on `emotions` that cannot be read as an `EmotionResult` SHALL be skipped with a log entry, and the following messages SHALL still be written.

#### Scenario: Invalid JSON between valid results
- **WHEN** a message that is not valid JSON sits between two valid results
- **THEN** both valid results are written and the invalid one produces no row

### Requirement: Independent of the classifier
The writer SHALL work through its own consumer group, so the classifier and the writer do not affect each other's position.

#### Scenario: Writer stopped
- **WHEN** the writer is not running while results keep being published, and is started later
- **THEN** it writes all the results it missed
