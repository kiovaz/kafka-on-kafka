## ADDED Requirements

### Requirement: Quote format
A `Quote` SHALL carry `id`, `text`, `book` and `part`, where `part` may be null. It SHALL serialize to and from JSON with exactly those field names.

#### Scenario: Quote round trip
- **WHEN** a `Quote` with a non-null `part` is serialized to JSON and read back
- **THEN** the result equals the original and the JSON has the keys `id`, `text`, `book` and `part`

#### Scenario: Quote without a section
- **WHEN** a `Quote` with a null `part` is serialized to JSON and read back
- **THEN** the result equals the original and `part` is null

### Requirement: Emotion categories
`Emotion` SHALL have exactly eight values: `anxiety`, `absurdity`, `resignation`, `alienation`, `bureaucracy`, `despair`, `confusion` and `unknown`. In JSON each value SHALL be its lowercase name.

#### Scenario: Lowercase in JSON
- **WHEN** `alienation` is serialized to JSON
- **THEN** the output is the string `"alienation"`, and reading it back gives the same value

### Requirement: EmotionResult format
An `EmotionResult` SHALL carry `quoteId`, `text`, `emotion`, `book`, `part` (nullable) and `classifiedAt`. In JSON, `classifiedAt` SHALL be an ISO-8601 UTC string such as `2026-10-03T14:22:10Z`.

#### Scenario: EmotionResult round trip
- **WHEN** an `EmotionResult` is serialized to JSON and read back
- **THEN** the result equals the original
- **AND** `classifiedAt` appears as an ISO-8601 string, not a number

### Requirement: Shared module build
The shared models SHALL build and pass their tests with `mvn verify` from the repository root, using Java 21.

#### Scenario: Root build
- **WHEN** `mvn verify` runs at the repository root
- **THEN** it succeeds and the JSON round-trip tests run
