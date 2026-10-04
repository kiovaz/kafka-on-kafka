## 1. Domain

- [x] 1.1 Add `Emotion.parse(String)` and `EmotionTest` covering the `domain-model` scenarios (exact, case/punctuation, first word only, sentence gives unknown, word outside the set, null/blank), verify the tests pass, and verify one fails when the first-word rule is removed

## 2. Ollama in Docker

- [x] 2.1 Add `ollama` (pinned `0.35.1`, port 11434, NVIDIA GPU reservation, `OLLAMA_KEEP_ALIVE=-1`, model volume, healthcheck) and `ollama-init` (pulls `OLLAMA_MODEL`, default `qwen2.5:3b`, loads it once, then exits) to `docker-compose.yml`, run `docker compose up -d`, and verify `ollama list` shows the model and `ollama-init` exited with 0
- [x] 2.2 Send one `POST /api/generate` request with the README prompt, verify it returns `response` and `done` true, and verify `ollama ps` shows the model on the GPU (100% GPU); if it does not fit, stop and report before going on
- [x] 2.3 Record the time of the first request (model loading) and of a second one (warm)
- [x] 2.4 Run `docker compose down` and `up -d` and verify the model is still listed and no new download happens

## 3. Classifier

- [x] 3.1 Add `EmotionClassifier` in `adapter.out.ollama` (two constructors, request per the spec, 3 attempts with a pause, `unknown` after the third failure, never throws) and `EmotionClassifierTest` with `MockRestServiceServer` (request content, success, fail twice then succeed with exactly 3 calls, fail three times gives unknown with exactly 3 calls), and verify the tests pass
- [x] 3.2 Add `EmotionResultPublisher` in `adapter.out.kafka` (topic `emotions`, key = `part`, waits for the acknowledgement) and verify it compiles
- [x] 3.3 Add `ClassifyQuote` in `application` and `ClassifyQuoteTest` with Mockito (result fields come from the quote and the classifier, `classifiedAt` is set, the result is published once), and verify the tests pass
- [x] 3.4 Add `QuoteListener` and the error-handling bean in `adapter.in.kafka`, and the consumer and `app.ollama.*` settings in `application.yml` (check the property names against the Spring Kafka sources), and verify `mvn verify` passes

## 4. Checks against the real infrastructure

- [x] 4.1 With the infrastructure up, run the app with `PRODUCER_DELAY_MS=300` and verify with `kafka-console-consumer` on `emotions` that every quote gets a result as JSON with lowercase emotions and the part as key, and note the count per emotion, how many are `unknown`, and the time per classification from the log
- [x] 4.2 Verify with `kafka-consumer-groups.sh --describe --group classifier` that the group exists and the lag goes down to 0
- [x] 4.3 Restart the app with `APP_PUBLISHER_ENABLED=false` and verify the number of messages in `emotions` does not grow (the classifier continues after its saved position instead of starting over)
- [x] 4.4 Stop the `ollama` container while quotes are being published and verify the results turn into `unknown` after 3 attempts without stopping the flow, then start it again and verify normal results come back
- [x] 4.5 Start with `OLLAMA_URL` pointing to a wrong address and verify the results are `unknown` and the log shows that address
- [x] 4.6 Publish a message that is not valid JSON to `quotes` between two valid ones and verify the invalid one is skipped with a log entry and both valid ones get results
- [x] 4.7 Delete the `classifier` group, delete and recreate `quotes` and `emotions` (`kafka-init`) so both are clean, and verify their offsets are 0

## 5. Documentation and wrap-up

- [x] 5.1 Update the README (Ollama in the Docker table with the GPU and the pinned version, `OLLAMA_URL`, `OLLAMA_MODEL`, `OLLAMA_TIMEOUT_SECONDS`, the measured results) and add the new decisions (Ollama on the GPU in Docker, unlimited retry for publishing, `earliest` plus `ErrorHandlingDeserializer`, `unknown` on outage) to `docs/DECISIONS.md`
- [x] 5.2 Verify `git status` shows no `target/` output, then run `openspec validate add-emotion-classifier`

## 6. Database moved to Neon (added during the apply)

- [x] 6.1 Add the hosted connection to a git-ignored `.env` and a versioned `.env.example`, make `application.yml` import `.env` and drop the database defaults, remove `postgres`, `adminer` and `pgdata` from `docker-compose.yml`, and verify the app starts and opens the connection pool with no warning and without the password in the log
- [x] 6.2 Update the README, `docs/DECISIONS.md` (decisions 10, 11 and the new 19) and the specs, and verify `git grep` finds no trace of the real password in tracked files
