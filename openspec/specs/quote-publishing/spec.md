# quote-publishing Specification

## Purpose
Defines how the quotes reach the `quotes` topic: format, key, pace, and what happens on failure, at the end of the book and when the publisher is turned off.

## Requirements

### Requirement: Message format
Each quote SHALL be sent to the topic `quotes` as a JSON object with the fields `id`, `text`, `book` and `part`, matching the example in the README, and the message key SHALL be the quote's `part`.

#### Scenario: Published message
- **WHEN** the quote `q-0001` of part `I` is published
- **THEN** the message value is JSON with `"id":"q-0001"` and a `"part":"I"` field
- **AND** the message key is `I`

#### Scenario: Same part, same partition
- **WHEN** several quotes of part `I` are published
- **THEN** they are all written to the same partition

### Requirement: Starts at startup and keeps a steady pace
After the application starts, the publisher SHALL publish one quote every `PRODUCER_DELAY_MS` milliseconds (default 3000), in book order, beginning with the first quote of the book.

#### Scenario: Pace
- **WHEN** the application runs with `PRODUCER_DELAY_MS=500`
- **THEN** consecutive messages are published about 500 ms apart or more

#### Scenario: Restart
- **WHEN** the application is stopped and started again
- **THEN** publishing starts again from the first quote

### Requirement: Stops at the end of the book
After the last quote has been published, the publisher SHALL publish nothing more during that run.

#### Scenario: End of book
- **WHEN** every quote has been published
- **THEN** no further messages are sent until the application is restarted

### Requirement: A failed publish does not skip a quote
A quote SHALL count as published only after Kafka acknowledges it. If publishing fails, the publisher SHALL try the same quote again on the next turn instead of moving on.

#### Scenario: Failure then recovery
- **WHEN** publishing quote 5 fails and the next attempt succeeds
- **THEN** quote 5 is the next one delivered, followed by quote 6

### Requirement: Can be turned off
With `APP_PUBLISHER_ENABLED=false` the application SHALL publish nothing and SHALL NOT read the book.

#### Scenario: Disabled
- **WHEN** the application starts with `APP_PUBLISHER_ENABLED=false`
- **THEN** no message is published to `quotes`
- **AND** the app starts even if the book config is invalid

### Requirement: Kafka address configurable
The publisher SHALL use the Kafka address from `KAFKA_BOOTSTRAP_SERVERS` when set, otherwise `localhost:29092`.

#### Scenario: Override
- **WHEN** the application starts with `KAFKA_BOOTSTRAP_SERVERS` set to another address
- **THEN** the Kafka producer is configured with that address
