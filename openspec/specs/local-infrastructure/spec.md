# local-infrastructure Specification

## Purpose
Gives every later service a local Kafka broker, pre-created topics and a browsable Postgres database, all started with one Docker Compose command.

## Requirements

### Requirement: Broker reachable from containers and from the host
The system SHALL run a single Kafka broker in KRaft mode (no Zookeeper) that other containers reach at `kafka:9092` and that host tools reach at `localhost:29092`.

#### Scenario: Container connects to the broker
- **WHEN** a container on the Compose network connects to `kafka:9092`
- **THEN** the connection succeeds and metadata is returned

#### Scenario: Host tool connects to the broker
- **WHEN** a tool on the host connects to `localhost:29092`
- **THEN** the connection succeeds and metadata is returned

### Requirement: Topics are created automatically
The system SHALL create the topics `quotes` and `emotions`, each with 3 partitions, when the stack starts, and `emotions` SHALL keep its messages indefinitely (`retention.ms=-1`). Running the stack again SHALL NOT fail or change topics that already exist.

#### Scenario: First start
- **WHEN** the stack starts with no existing data
- **THEN** `quotes` and `emotions` exist with 3 partitions each
- **AND** `emotions` has `retention.ms=-1`

#### Scenario: Second start
- **WHEN** the stack is started again with the topics already present
- **THEN** the topic-creation step completes successfully and the topics are unchanged

### Requirement: Kafka data survives restarts
The system SHALL keep Kafka topics and their messages across `docker compose down` and `up`, and SHALL only delete them when the volumes are removed explicitly.

#### Scenario: Restart without volume removal
- **WHEN** a message is published to `quotes` and the stack is stopped with `docker compose down` and started again
- **THEN** the topics keep the same TopicId and the message can still be read from the beginning

### Requirement: Ollama with the model, on the GPU
The stack SHALL run Ollama (a pinned version) on host port 11434, configured to use the machine's NVIDIA GPU, and SHALL download the model named by `OLLAMA_MODEL` (default `qwen2.5:3b`) automatically on the first start with a one-shot container that then exits successfully. The downloaded model SHALL be kept across `docker compose down` and `up`.

#### Scenario: First start
- **WHEN** the stack starts for the first time
- **THEN** the model is downloaded and listed by Ollama
- **AND** the download container exits with code 0

#### Scenario: Later starts
- **WHEN** the stack is stopped with `docker compose down` and started again
- **THEN** the model is still listed and is not downloaded again

#### Scenario: Answering a request
- **WHEN** a request is sent to `POST http://localhost:11434/api/generate` with the model, a prompt and `stream` false
- **THEN** the response contains a `response` text and `done` true

#### Scenario: Running on the GPU
- **WHEN** the model has answered a request on a machine with an NVIDIA GPU
- **THEN** Ollama reports the model as running on the GPU

#### Scenario: Model ready after start
- **WHEN** the download container has finished
- **THEN** the model is already loaded and stays loaded, so the first request does not wait for the model to load
