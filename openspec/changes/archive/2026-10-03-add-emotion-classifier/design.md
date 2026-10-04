## Context

The publisher writes quotes to `quotes`. The README and `docs/DECISIONS.md` fix the rules: hexagonal package layout, no interface without a second implementation, Spring Kafka, group `classifier`, `max.poll.records=1`, retry 3 times then `unknown` (decisions 2, 3, 6, 8). See proposal.md for scope.

Facts checked on this machine: 7.7 GB of RAM in total, Docker Desktop limited to 3.9 GB; an NVIDIA RTX 4050 Laptop GPU with 6 GB of VRAM, and a container started with `--gpus all` sees it. The Windows host had only about 1 GB of free RAM while Docker and the IDE were running, and the first attempt with `qwen2.5:7b` (4.7 GB) made the machine freeze, so the default is the lighter `qwen2.5:3b` (about 1.9 GB), which fits comfortably in the 6 GB of VRAM. The newest stable `ollama/ollama` image is 0.35.1. Spring Kafka 4.1.1 has `JacksonJsonDeserializer` and `ErrorHandlingDeserializer`.

## Goals / Non-Goals

**Goals:**
- Every quote becomes a result in `emotions`, in the order Kafka gives them per partition, and a slow or failing model never blocks the topic.
- No result is lost when Kafka hiccups; a bad message is skipped.
- The parsing and the retry logic are covered by fast tests without Ollama or Kafka.

**Non-Goals:**
- Consuming `emotions` (blocks 5-6), packaging the app in Docker (block 7), several classifier instances (block 8 experiments).
- Tuning the prompt or measuring the model's accuracy.
- Running Ollama outside Docker.

## Decisions

**Ollama in Docker, on the GPU.** The `ollama` service reserves the NVIDIA device (`deploy.resources.reservations.devices` with driver `nvidia`, capabilities `gpu`), pins `ollama/ollama:0.35.1`, stores models in a named volume and has a healthcheck (`ollama list`). The model name comes from `OLLAMA_MODEL` (default `qwen2.5:3b`) and is the same variable the app reads, so switching to a smaller model is one variable. Rejected: Ollama on CPU in Docker (the model does not fit in the 3.9 GB Docker RAM and would be far too slow), and Ollama installed natively on Windows (breaks "everything runs in Docker", decision 11). Rejected: the 7B model (`qwen2.5:7b`), which was tried first and made the machine freeze (see Context); it can still be chosen with `OLLAMA_MODEL`.

**A one-shot `ollama-init` container** runs `ollama pull $OLLAMA_MODEL` against the `ollama` service once it is healthy, then `ollama run $OLLAMA_MODEL ok` once to load the model, and exits. The `ollama` service sets `OLLAMA_KEEP_ALIVE=-1` so the model stays on the GPU. Measured with `qwen2.5:3b` here: the first request after a cold start took 149 s (loading the model with the Windows host short on RAM), every later one about 0.14 s, and by default Ollama would also unload the model after 5 idle minutes. Loading it in the init container and keeping it loaded moves that wait out of the application's first message. The pull is skipped quickly when the model is already in the volume.

**Five small classes, no interfaces:**
- `domain.Emotion.parse(String)`: the vocabulary rule, pure Java.
- `adapter.out.ollama.EmotionClassifier`: builds the request, calls Ollama, parses the answer, retries 3 times with a pause, returns `unknown` after the third failure; it never throws.
- `adapter.out.kafka.EmotionResultPublisher`: same shape as `QuotePublisher` (send, wait for the acknowledgement), for the `emotions` topic. The few lines of send-and-wait are repeated instead of shared; two copies are cheaper than an abstraction.
- `application.ClassifyQuote`: `handle(Quote)` asks the classifier, builds the `EmotionResult` with the current time and publishes it.
- `adapter.in.kafka.QuoteListener`: the `@KafkaListener` that calls `ClassifyQuote`.

**`EmotionClassifier` has two constructors:** one for Spring (reads `app.ollama.*` and builds a `RestClient` with a JDK `HttpClient` and the read timeout) and one that takes a ready `RestClient`, the model and the pause. Tests use the second with `MockRestServiceServer`, and a zero pause, so they are instant. This avoids depending on Spring Boot's HTTP client property names.

**Consumer settings in `application.yml`:** group `classifier`, `auto-offset-reset: earliest` (so a new group reads what the publisher already wrote), `max-poll-records: 1` (a slow model stays far below `max.poll.interval.ms`), keys as `String`, and values through `ErrorHandlingDeserializer` wrapping `JacksonJsonDeserializer` with default type `Quote`, `spring.json.use.type.headers=false` and trusted package `com.kiovaz.kafkaonkafka.domain`. The `ErrorHandlingDeserializer` turns an unreadable message into an error the container can skip instead of failing the poll forever.

**Error handling: unlimited retry with a pause, except for unreadable messages.** A small `@Bean` (`DefaultErrorHandler` with `FixedBackOff` of 2 s and unlimited attempts, in `adapter.in.kafka`) makes the container retry the same message when the listener throws, which only happens when publishing the result fails. Deserialization errors are not retried by this handler by default, so they are logged and skipped. Rejected: Spring's default (10 immediate attempts, then skip), which would lose a result in a short Kafka outage.

**Failure policy: retry what can fix itself, give `unknown` only when retrying cannot help.** After 3 failed attempts `EmotionClassifier` throws when the cause can fix itself (Ollama unreachable, a timeout, a 5xx error or an unreadable answer); the listener then fails and the `DefaultErrorHandler` retries the same message every 2 s without limit, so the topic waits for Ollama and no result is recorded for the outage. A 4xx error (the request itself is wrong) gives `unknown` after 3 attempts, because retrying forever would stall the partition on a message that can never succeed, and an answer outside the emotion set is also `unknown` (the model did answer). This replaces the first version of decision 8, which recorded `unknown` after any 3 failures and turned a one-minute outage into 34 permanent wrong results.

**Configuration:** `app.ollama.url` (`OLLAMA_URL`, default `http://localhost:11434`), `app.ollama.model` (`OLLAMA_MODEL`), `app.ollama.timeout-seconds` (`OLLAMA_TIMEOUT_SECONDS`, default 120: the first call also loads the model into the GPU) and `app.ollama.retry-delay-ms` (default 1000).

**`Emotion.parse`** splits the cleaned text on non-letters and takes the first word, so `Alienation.` works and `The tone is despair` does not (it gives `unknown`, as the README states). The real unknown rate is measured in the checks.

**Tests:** `EmotionTest` for `parse`; `EmotionClassifierTest` with `MockRestServiceServer` (request content, success, fail-twice-then-succeed, fail three times, exactly three calls); `ClassifyQuoteTest` with Mockito (result fields and publish). The listener wiring is verified against the real infrastructure, including a bad message.

## Risks / Trade-offs

- [The model may not run fully on the GPU, so part of it runs on the CPU, slowly] → Check `ollama ps` for 100% GPU; if not, stop and report.
- [Docker's 3.9 GB of RAM is shared by Kafka, Postgres, Adminer and Ollama's runtime] → Watch for out-of-memory restarts during the checks; raising Docker's memory is a user setting.
- [Property names for the JSON deserializer and `ErrorHandlingDeserializer` may differ in Spring Boot 4] → Check against the Spring Kafka sources first, as done for the serializer.
- [A long Ollama outage stalls the `classifier` group] → Accepted: the topic waits and no result is lost or wrong; `quotes` just accumulates lag and drains after Ollama is back. A quote that Ollama keeps failing on with a 5xx would also stall its partition, visible in the logs.
- [The first call is slow while the model loads into the GPU] → The 120 s timeout covers it; the check records cold and warm times.
- [The model download is about 1.9 GB] → One time; the model is kept in a volume.
- [A restart can reclassify the last message (at-least-once)] → The writer will ignore the duplicate (decision 10).
