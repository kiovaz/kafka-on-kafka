## ADDED Requirements

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

## REMOVED Requirements

### Requirement: Database is available and browsable
**Reason**: The database moved to Neon, a hosted PostgreSQL (decision 19), so the stack no longer runs Postgres or Adminer.
**Migration**: Use the hosted database through `DB_URL`, `DB_USER` and `DB_PASSWORD`, and browse it in the Neon console.

### Requirement: Database data survives restarts
**Reason**: There is no local database volume any more; the data lives in Neon.
**Migration**: None needed; the hosted database keeps its own data.
