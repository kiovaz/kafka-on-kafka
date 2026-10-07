## Purpose

Defines how the application is built into a container image from the repository and run as part of the Compose stack with one command.

## ADDED Requirements

### Requirement: Image built from the repository
`docker compose build app` SHALL build the application image from the repository alone, with no Java or Maven needed on the host, without running the tests, and the image SHALL run as a non-root user.

#### Scenario: Clean build
- **WHEN** `docker compose build app` is run on a machine with only Docker
- **THEN** the build succeeds and produces an image that starts the application

#### Scenario: Non-root
- **WHEN** the container is running
- **THEN** the application process is not running as root

### Requirement: No secret in the image
The image SHALL NOT contain the `.env` file or any database credential. The database settings SHALL reach the container only as environment variables at run time.

#### Scenario: Image contents
- **WHEN** the files of the built image are listed
- **THEN** there is no `.env` file

#### Scenario: Run-time settings
- **WHEN** the container starts
- **THEN** `DB_URL`, `DB_USER` and `DB_PASSWORD` come from the `.env` file on the host through the Compose service definition

### Requirement: Wired to the other services
In the Compose stack the app container SHALL use Kafka at `kafka:9092` and Ollama at `http://ollama:11434`, SHALL read the book from the host's `data` folder mounted read-only, and SHALL publish port 8080 to the host.

#### Scenario: Pipeline in containers
- **WHEN** the whole stack is started with `docker compose up --build`
- **THEN** the quotes are published, classified and written to the database, and `http://localhost:8080` shows the classifications

#### Scenario: Other book without rebuilding
- **WHEN** the files in the host's `data` folder are changed and the app container is restarted
- **THEN** the app reads the changed files without rebuilding the image

### Requirement: Starts after its dependencies are ready
The app container SHALL start only after the topics have been created and the model has been downloaded and loaded.

#### Scenario: First start
- **WHEN** the stack starts from nothing
- **THEN** the app container starts after the topic creation and the model warm-up have finished successfully

### Requirement: Limited memory
The app container SHALL have a memory limit of 640 MB and the Java heap SHALL be capped at 384 MB, so the app, Kafka and Ollama fit in the Docker memory of the development machine.

#### Scenario: Limits applied
- **WHEN** the app container is running
- **THEN** its memory limit is 640 MB and the JVM maximum heap is 384 MB
