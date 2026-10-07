## Context

The app runs from the IDE against the stack in Docker. Kafka and Ollama are in Compose; the database is Neon, reached with settings from `.env` (decision 19); the book is read from `data/` (`BOOK_CONFIG_PATH`, default `data/book-config.json`). The Docker VM has about 3.9 GB (the machine has 7.7 GB). Checked on Docker Hub: `maven:3.9.16-eclipse-temurin-21` and `eclipse-temurin:21.0.12_8-jre` exist. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- `docker compose up --build` runs the whole pipeline.
- No secret ever baked into the image.
- The app stays small enough to share the Docker memory with Kafka and Ollama.

**Non-Goals:**
- Running tests during the image build, a health check, restart policies, layered jars or a custom base image.
- Running the IDE copy and the container copy at the same time (they would split the consumer groups; that is the experiment of block 8, done on purpose).

## Decisions

**Two stages.** `maven:3.9.16-eclipse-temurin-21` builds `mvn -B -DskipTests package` (the Spring Boot plugin makes the runnable jar; the enforcer rule is satisfied by the same Java 21 and Maven 3.9.x used on the host), and `eclipse-temurin:21.0.12_8-jre` runs it. Tests run on the host, so an image build never depends on them. A BuildKit cache mount on `/root/.m2` keeps repeated builds from downloading every dependency again. Rejected: building the jar on the host and copying it (needs a Java and Maven on the host), and running the tests in the image build (slower, and no extra safety).

**Non-root user** created in the run stage, because it costs two lines.

**`.dockerignore` keeps `.env` out of the build context.** The secrets stay on the host and are passed with `env_file: .env` when the container starts, so they exist only in the container's environment. `docker compose config` expands `env_file` values, so it should not be pasted anywhere; noted in the README.

**The book data is mounted, not copied:** `./data:/app/data:ro`. Switching books then needs no rebuild, as the README promises, and the working directory `/app` makes the default `data/book-config.json` resolve.

**Service wiring through environment variables the app already reads:** `KAFKA_BOOTSTRAP_SERVERS=kafka:9092` and `OLLAMA_URL=http://ollama:11434`. The `.env` import in `application.yml` is `optional:`, so a missing file inside the container is not an error.

**`depends_on` with `condition: service_completed_successfully`** for `kafka-init` and `ollama-init`. `ollama-init` only finishes after it has pulled and loaded the model, so the classifier never meets a cold model.

**Memory:** `mem_limit: 640m` for the container and `JAVA_TOOL_OPTIONS=-Xmx384m` for the heap. The rest of the JVM (metaspace, threads, buffers) fits in the remaining 256 MB for an app this small. If the checks show the container being killed, raise the limit first.

**Port 8080 published,** for the live page.

## Risks / Trade-offs

- [The Docker VM is tight: Kafka, Ollama and the app together may not fit and Docker may crash again] → Memory is capped for the app; the final check watches `docker stats`; if it does not fit, stop the Ollama container between runs or use a smaller heap.
- [The first build is slow because every dependency is downloaded] → Accepted; the cache mount makes the next builds fast.
- [Neon scale-to-zero can make the first connection slow, and the app needs the database at startup] → Accepted; noted in the README.
- [Running the IDE copy and the container copy together splits the partitions and duplicates the published book] → Documented in the README as something not to do by accident.
- [The image build needs network access] → Accepted.
