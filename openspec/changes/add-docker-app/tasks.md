> **Status:** the files are written. Nothing has been built or run yet (decision of the owner: run everything once all blocks are coded), so no task below is ticked until it is verified in that run.

## 1. Image

- [ ] 1.1 Add the multi-stage `Dockerfile` (build stage with the cache mount and `-DskipTests`, run stage with the JRE, non-root user) and the `.dockerignore` (`.env`, `.git`, `target`, IDE and tool folders, docs, `data`), and verify `docker compose build app` succeeds

## 2. Compose

- [ ] 2.1 Add the `app` service to `docker-compose.yml` (build, `depends_on` with `service_completed_successfully` for `kafka-init` and `ollama-init`, `env_file: .env`, `KAFKA_BOOTSTRAP_SERVERS`, `OLLAMA_URL`, `JAVA_TOOL_OPTIONS`, the `data` mount, port 8080, `mem_limit`), and verify the file is valid and the services and their order are as expected

## 3. Documentation

- [ ] 3.1 Update the README (run the whole stack with one command, the `.env` requirement, do not run the IDE copy at the same time, do not paste `docker compose config`) and add decision 24 to `docs/DECISIONS.md`, and run `openspec validate add-docker-app`

## 4. Final checks (run at the end of the whole development; need Docker and Neon)

- [ ] 4.1 Verify the built image contains no `.env` file and that the process does not run as root
- [ ] 4.2 Start everything from scratch with `docker compose up --build` and verify the app starts only after `kafka-init` and `ollama-init` have finished
- [ ] 4.3 Verify the pipeline in containers: quotes published, classified, rows in Neon, and `http://localhost:8080` updating live
- [ ] 4.4 Verify with `docker stats` that the app stays within its 640 MB limit and the whole stack fits in the Docker memory
- [ ] 4.5 Change the files in `data/`, restart the app container and verify it reads them without a rebuild
