## ADDED Requirements

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
