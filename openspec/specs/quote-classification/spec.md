# quote-classification Specification

## Purpose
Defines how quotes are read from the `quotes` topic, classified by the local language model and published as results to the `emotions` topic, including how failures are handled.

## Requirements

### Requirement: Reading the quotes
The application SHALL read the topic `quotes` as the consumer group `classifier`, one message at a time. A group with no saved position SHALL start from the oldest message, and a group with a saved position SHALL continue after it.

#### Scenario: First start after the publisher
- **WHEN** the application starts for the first time and `quotes` already holds messages
- **THEN** it classifies those messages, starting from the first

#### Scenario: Restart
- **WHEN** the application is restarted after classifying some messages
- **THEN** it continues after the last processed message instead of starting over

### Requirement: Asking the model
For each quote the application SHALL send `POST {OLLAMA_URL}/api/generate` (default `http://localhost:11434`) with the model `OLLAMA_MODEL` (default `qwen2.5:3b`), `stream` false, the option `temperature` set to 0 (so the same excerpt always gets the same answer) and the prompt from the README (the instruction listing the seven emotions followed by the excerpt text), and SHALL read the `response` field of the answer.

#### Scenario: Request content
- **WHEN** a quote is classified
- **THEN** the request carries the configured model, `stream` false, `options.temperature` 0 and a prompt containing the quote's text and the seven emotion words

#### Scenario: Same excerpt, same answer
- **WHEN** the same excerpt is classified twice
- **THEN** both requests are identical, with the temperature at 0

### Requirement: Emotion from the answer
The emotion of a result SHALL be the one read from the model's answer as defined in the domain model.

#### Scenario: Normal answer
- **WHEN** the model answers `alienation.`
- **THEN** the result's emotion is alienation

### Requirement: Retries, then wait or unknown
If the call to the model fails, the application SHALL try again, up to 3 attempts in total with a short pause between them. A single request SHALL wait at most `OLLAMA_TIMEOUT_SECONDS` (default 120). If all 3 attempts fail because of something that can fix itself (Ollama unreachable, a timeout, a server error or an unreadable answer), the application SHALL NOT record a result: it SHALL keep trying the same quote again later (waiting between rounds, without limit) until Ollama answers, and the following quotes wait behind it. If the model rejects the request itself (a client error, which retrying cannot fix), the result's emotion SHALL be `unknown`. If the model answers but the answer is outside the emotion set, the result's emotion SHALL be `unknown` without retrying.

#### Scenario: Recovers on the third attempt
- **WHEN** the first two calls fail and the third answers `despair`
- **THEN** the result's emotion is despair, after exactly 3 calls

#### Scenario: Ollama is down
- **WHEN** Ollama cannot be reached
- **THEN** no result is published for that quote and no quote after it is processed
- **AND** when Ollama is reachable again, the same quote is classified normally and no quote is skipped

#### Scenario: Request rejected
- **WHEN** all 3 calls are rejected with a client error
- **THEN** the result's emotion is unknown, after exactly 3 calls, and the result is still published

#### Scenario: Answer outside the set
- **WHEN** the model answers `joy` on the first call
- **THEN** the result's emotion is unknown, after exactly 1 call

### Requirement: Publishing the result
For each quote the application SHALL publish to the topic `emotions` a JSON `EmotionResult` with the quote's `id` as `quoteId`, its `text`, `book` and `part`, the emotion (lowercase in JSON) and `classifiedAt` set to the time of classification, matching the README example. The message key SHALL be the quote's `part`. The quote SHALL count as processed only after Kafka acknowledges the result.

#### Scenario: Published result
- **WHEN** the quote `q-0001` of part `I` is classified as alienation
- **THEN** a message with key `I` and JSON containing `"quoteId":"q-0001"` and `"emotion":"alienation"` is written to `emotions`

### Requirement: No result is lost
If publishing a result fails, the application SHALL try the same quote again (waiting between attempts, without a limit) instead of skipping it.

#### Scenario: Kafka briefly unavailable
- **WHEN** publishing the result of a quote fails and works a moment later
- **THEN** that quote's result appears in `emotions`, and the following quotes are classified after it

### Requirement: A bad message does not block
A message on `quotes` that cannot be read as a `Quote` SHALL be skipped with a log entry, and the following messages SHALL still be classified.

#### Scenario: Invalid JSON between valid quotes
- **WHEN** a message that is not valid JSON sits between two valid quotes
- **THEN** both valid quotes are classified and the invalid one produces no result
