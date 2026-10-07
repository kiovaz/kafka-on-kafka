## MODIFIED Requirements

### Requirement: Topics are created automatically
The system SHALL create the topics `quotes` and `emotions`, each with 3 partitions, when the stack starts, and `emotions` SHALL keep its messages indefinitely (`retention.ms=-1`). Running the stack again SHALL NOT fail or change topics that already exist with that configuration. The broker SHALL NOT create topics on its own when a client uses a name that does not exist, and the topic creation step SHALL fail, with a message naming the topic, when an existing topic does not have 3 partitions or `emotions` does not have `retention.ms=-1`.

#### Scenario: First start
- **WHEN** the stack starts with no existing data
- **THEN** `quotes` and `emotions` exist with 3 partitions each
- **AND** `emotions` has `retention.ms=-1`

#### Scenario: Second start
- **WHEN** the stack is started again with the topics already present
- **THEN** the topic-creation step completes successfully and the topics are unchanged

#### Scenario: A client asks for a missing topic
- **WHEN** a client reads or writes a topic that does not exist
- **THEN** the broker does not create it

#### Scenario: A wrong topic already exists
- **WHEN** a topic named `quotes` or `emotions` already exists with a different number of partitions, or `emotions` without `retention.ms=-1`
- **THEN** the topic-creation step exits with an error naming the topic
