## Purpose

Defines how the application starts and finds Kafka and PostgreSQL, so it runs from the IDE against the Docker infrastructure and inside Docker with only environment variables changed.

## ADDED Requirements

### Requirement: Starts against the local infrastructure
With the Docker infrastructure running, the application SHALL start with no extra configuration and serve HTTP on port 8080, connecting to Kafka at `localhost:29092` and to PostgreSQL at `localhost:5432` (database, user and password `kafka`).

#### Scenario: Start from the IDE or Maven
- **WHEN** `docker compose up -d` has finished and the application is started from the host
- **THEN** it starts without errors and answers HTTP requests on port 8080

### Requirement: Connections overridable by environment
The Kafka address and the database URL, user and password SHALL be overridable by environment variables, so the same build runs inside Docker using `kafka:9092` and `postgres:5432`.

#### Scenario: Override the Kafka address
- **WHEN** the application is started with `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`
- **THEN** it uses that address instead of `localhost:29092`

#### Scenario: Override the database
- **WHEN** the application is started with `DB_URL`, `DB_USER` and `DB_PASSWORD` set
- **THEN** it uses those values instead of the defaults
