## Why

The publisher fills the `quotes` topic, but nothing reads it. The classifier is the heart of the pipeline: it asks the local LLM for the emotional tone of each excerpt and publishes the result to `emotions`, which the writer and the live feed will consume in blocks 5 and 6. See `docs/DECISIONS.md` (3, 6, 8).

## What Changes

- Add `Emotion.parse` (`domain`): turns the model's raw answer into an `Emotion`, falling back to `unknown`.
- Add `EmotionClassifier` (`adapter.out.ollama`): calls Ollama, retries 3 times, never throws; after 3 failures it returns `unknown`.
- Add `EmotionResultPublisher` (`adapter.out.kafka`): sends an `EmotionResult` to `emotions`, keyed by `part`, waiting for the acknowledgement.
- Add `ClassifyQuote` (`application`): classifies a quote and publishes its result.
- Add `QuoteListener` and an error-handling config (`adapter.in.kafka`): consumes `quotes` as group `classifier`, one message at a time, and does not lose a result when publishing fails.
- Add Ollama and a one-shot model download to `docker-compose.yml`, using the NVIDIA GPU.
- Add the consumer and `app.ollama.*` configuration, tests, and update the README and `docs/DECISIONS.md`.
- No new dependencies, no interfaces.

## Capabilities

### New Capabilities
- `quote-classification`: how quotes are consumed, classified and published as results, and how failures are handled.

### Modified Capabilities
- `domain-model`: adds how the model's raw answer becomes an `Emotion`.
- `local-infrastructure`: adds Ollama with the model, running on the GPU.

## Impact

- New classes under `src/main/java/com/kiovaz/kafkaonkafka/{application,adapter}`, new properties in `application.yml`, new tests.
- `docker-compose.yml` gains `ollama` and `ollama-init` and a volume for the model.
- The first start downloads the model (about 1.9 GB for `qwen2.5:3b`).
- Machine facts that shape this: 7.7 GB of RAM (Docker Desktop is limited to 3.9 GB) and an NVIDIA RTX 4050 with 6 GB of VRAM that Docker can see. The first attempt with the 7B model froze the machine, so the default model is the lighter `qwen2.5:3b`.
- The app, run from the IDE, now reads from Kafka and calls Ollama at `localhost:11434`.
