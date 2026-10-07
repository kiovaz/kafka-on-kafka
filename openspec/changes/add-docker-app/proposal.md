## Why

Today the app runs from the IDE. For "everything runs with one command" the app itself has to be a container in the same Compose stack, built from the repository, so anyone with Docker can run the whole pipeline. See `docs/DECISIONS.md` (11, 19).

## What Changes

- Add a multi-stage `Dockerfile`: a Maven + JDK 21 stage builds the jar (tests skipped, they run on the host), a JRE 21 stage runs it as a non-root user.
- Add a `.dockerignore` that keeps secrets and noise out of the build (`.env`, `.git`, `target`, IDE and tool folders, docs).
- Add the `app` service to `docker-compose.yml`: built from the repository, started after the topics exist and the model is ready, talking to `kafka:9092` and `http://ollama:11434`, database settings from `.env`, the book data mounted read-only from `./data`, port 8080 published, memory capped.
- Update the README (how to run the whole stack, and not to run the IDE copy at the same time) and `docs/DECISIONS.md`.
- Nothing is built or run in this change; the checks are the last tasks, done at the end of the whole development.

## Capabilities

### New Capabilities
- `app-container`: how the app is built into an image and run in the Compose stack.

### Modified Capabilities

## Impact

- New: `Dockerfile`, `.dockerignore`. Changed: `docker-compose.yml`, README, decisions.
- `docker compose up --build` now brings up Kafka, Ollama and the app; the database stays on Neon.
- Memory: the Docker VM has about 3.9 GB. Kafka (about 0.3 to 0.5 GB), Ollama (about 2 GB with the model loaded) and the app (capped at 640 MB, heap 384 MB) should fit, with little to spare.
- The image must never contain `.env`; the database settings reach the container only as environment variables at run time.
