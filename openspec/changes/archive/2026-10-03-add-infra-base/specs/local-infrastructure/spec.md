## Purpose

Gives every later service a local Kafka broker, pre-created topics and a browsable Postgres database, all started with one Docker Compose command.

## ADDED Requirements

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

### Requirement: Database is available and browsable
The system SHALL run PostgreSQL 16 on host port 5432 and a web database UI (Adminer) on host port 8081 that can open the Postgres database.

#### Scenario: Open the database in the browser
- **WHEN** a user opens `http://localhost:8081` and logs in with system `PostgreSQL`, server `postgres` and the configured credentials
- **THEN** the database's tables are listed (empty until the writer service exists)

### Requirement: Kafka data survives restarts
The system SHALL keep Kafka topics and their messages across `docker compose down` and `up`, and SHALL only delete them when the volumes are removed explicitly.

#### Scenario: Restart without volume removal
- **WHEN** a message is published to `quotes` and the stack is stopped with `docker compose down` and started again
- **THEN** the topics keep the same TopicId and the message can still be read from the beginning

### Requirement: Database data survives restarts
The system SHALL keep Postgres data across `docker compose down` and `up`, and SHALL only delete it when the volumes are removed explicitly.

#### Scenario: Restart without volume removal
- **WHEN** the stack is stopped with `docker compose down` and started again
- **THEN** previously stored database data is still present
