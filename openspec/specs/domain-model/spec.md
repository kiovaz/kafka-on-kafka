# domain-model Specification

## Purpose
Defines the message types used across the application and their JSON form on the Kafka topics, so every part agrees on the same format.

## Requirements

### Requirement: Quote message
The `domain` package SHALL define `Quote` with the fields `id`, `text`, `book` and `part`, where `part` MAY be null.

#### Scenario: JSON round trip
- **WHEN** a `Quote` is serialized to JSON and read back
- **THEN** the result equals the original, including when `part` is null

### Requirement: Emotion values
The `domain` package SHALL define the closed set of emotions: `anxiety`, `absurdity`, `resignation`, `alienation`, `bureaucracy`, `despair`, `confusion` and `unknown`. In JSON each emotion SHALL be its lowercase name, and a value outside the set SHALL be rejected.

#### Scenario: Lowercase in JSON
- **WHEN** the alienation emotion is serialized to JSON
- **THEN** it appears as the string `"alienation"`

#### Scenario: Unknown fallback is a valid value
- **WHEN** JSON containing `"unknown"` is read
- **THEN** it becomes the unknown emotion

#### Scenario: Value outside the set
- **WHEN** JSON containing an emotion such as `"joy"` is read
- **THEN** reading fails

### Requirement: EmotionResult message
The `domain` package SHALL define `EmotionResult` with the fields `quoteId`, `text`, `emotion`, `book`, `part` (nullable) and `classifiedAt`, and its JSON form SHALL match the example in the README.

#### Scenario: JSON form
- **WHEN** an `EmotionResult` with emotion alienation and classifiedAt `2026-10-03T14:22:10Z` is serialized
- **THEN** the JSON contains `"emotion":"alienation"` and `"classifiedAt":"2026-10-03T14:22:10Z"`

#### Scenario: JSON round trip
- **WHEN** an `EmotionResult` is serialized to JSON and read back
- **THEN** the result equals the original, including when `part` is null

### Requirement: Domain has no framework dependency
Classes in the `domain` package SHALL NOT depend on Spring or on any Kafka, persistence or HTTP library. The only library annotation allowed is the JSON annotation that sets the emotion's JSON name.

#### Scenario: Imports
- **WHEN** the imports of the classes in `domain` are listed
- **THEN** none of them starts with `org.springframework`, `org.apache.kafka`, `jakarta.persistence` or `org.hibernate`

### Requirement: Reading the model's answer
`Emotion` SHALL provide a way to turn the raw text answered by the language model into an emotion. The text SHALL be trimmed, lowercased and stripped of punctuation, only its first word SHALL be considered, and a word outside the closed set, or a null or blank text, SHALL give `unknown`.

#### Scenario: Exact word
- **WHEN** the answer is `alienation`
- **THEN** the emotion is alienation

#### Scenario: Case, spaces and punctuation
- **WHEN** the answer is `  Alienation.\n`
- **THEN** the emotion is alienation

#### Scenario: Only the first word counts
- **WHEN** the answer is `despair and fear`
- **THEN** the emotion is despair

#### Scenario: A sentence is not an answer
- **WHEN** the answer is `The tone is despair`
- **THEN** the emotion is unknown

#### Scenario: Word outside the set
- **WHEN** the answer is `joy`
- **THEN** the emotion is unknown

#### Scenario: Empty answer
- **WHEN** the answer is null or blank
- **THEN** the emotion is unknown
