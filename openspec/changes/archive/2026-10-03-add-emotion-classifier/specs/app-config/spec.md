## MODIFIED Requirements

### Requirement: Starts against the local infrastructure
With the Docker infrastructure (Kafka and Ollama) running and the database connection configured, the application SHALL start and serve HTTP on port 8080, connecting to Kafka at `localhost:29092` and to the hosted PostgreSQL named by the database settings.

#### Scenario: Start from the IDE or Maven
- **WHEN** `docker compose up -d` has finished, the database settings are available and the application is started from the host
- **THEN** it starts without errors and answers HTTP requests on port 8080

#### Scenario: Missing database settings
- **WHEN** the application is started without `DB_URL`, `DB_USER` and `DB_PASSWORD` from the environment or the `.env` file
- **THEN** it fails to start with a message naming the missing setting

### Requirement: Connections overridable by environment
The Kafka address SHALL be overridable by the environment variable `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:29092`), so the same build runs inside Docker using `kafka:9092`. The database URL, user and password SHALL have no default and SHALL come from the environment variables `DB_URL`, `DB_USER` and `DB_PASSWORD` or from a `.env` file in the project root.

#### Scenario: Override the Kafka address
- **WHEN** the application is started with `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`
- **THEN** it uses that address instead of `localhost:29092`

#### Scenario: Override the database
- **WHEN** the application is started with `DB_URL`, `DB_USER` and `DB_PASSWORD` set in the environment
- **THEN** it connects to that database

#### Scenario: Database settings from the .env file
- **WHEN** `DB_URL`, `DB_USER` and `DB_PASSWORD` are set only in a `.env` file in the project root
- **THEN** the application connects to that database

## ADDED Requirements

### Requirement: No secret in versioned files
The database password SHALL NOT appear in any file tracked by Git or in the application log. The `.env` file SHALL be ignored by Git, and a `.env.example` with placeholders SHALL be versioned.

#### Scenario: Searching the repository
- **WHEN** tracked files are searched for the real password
- **THEN** it is not found

#### Scenario: Startup log
- **WHEN** the application starts and connects to the database
- **THEN** the log does not contain the password
