# Runbook: the checks that still need the real system

Everything here needs Docker, Kafka, Ollama and Neon, so it is run by hand, in this order. Run from the repository root in Git Bash unless it says PowerShell. Open a second window with the app log (`Get-Content -Wait` on the log file, or the IDE console) before each step that starts the app. Tick the matching line in the `tasks.md` of the change **only after** seeing the expected result.

## Already verified (do not repeat)

| What | How it was seen |
| --- | --- |
| 47 unit tests | `mvn test`, all passed |
| Pipeline Kafka → Ollama → `emotions` → Neon + live page | one full run from the IDE: 97 quotes, 97 rows, `/stats` equal to the table |
| Events numbered 1 to 97 in order, no duplicate in the page | SSE stream read during that run |
| An invalid message on `emotions` is skipped | published by hand between two valid ones |
| Classifier retries and `unknown` rules | unit tests with a mock server |

## 0. Before anything

1. Close what you do not need (Docker VM has about 3.9 GB). Start Docker Desktop.
2. Reset the Neon password in the console (it was pasted in a chat) and update `DB_PASSWORD` in `.env`.
3. `docker compose up -d kafka` — recreates the broker with auto-create **off**.
4. Make sure no app runs: `powershell scripts/stop-app.ps1` → must print `port 8080 is free`.
5. `bash scripts/reset-topics.sh` → expected last lines: `PartitionCount: 3` for `quotes` and for `emotions`, and `retention.ms=-1` shown for `emotions` in the describe below. If `kafka-init` prints `ERROR: topic ...`, the topic is wrong: run the script again.
6. `docker compose up -d ollama ollama-init` and wait until `docker compose ps -a` shows `ollama-init` as `Exited (0)`.
7. Empty the table: `bash scripts/neon-sql.sh "TRUNCATE TABLE classified_quotes RESTART IDENTITY"` and check `SELECT count(*)` returns `0`.
8. Delete the old consumer groups so everything is read from the start:
   `for g in classifier results-writer frontend; do docker compose exec -T kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --delete --group $g; done` (a group that does not exist gives an error: ignore it).

## 1. Blocks 5 and 6 (app from the IDE)

Start the app from the IDE with the publisher on (default). Wait about 5 minutes (97 quotes, 3 s apart by default; set `PRODUCER_DELAY_MS=1000` to make it about 2 minutes).

| Check | Command or action | Expected |
| --- | --- | --- |
| 5.2 restart does not grow the table | stop the app (`scripts/stop-app.ps1`), start it again, wait until the log shows the quotes published again and the classifier idle, then `bash scripts/neon-sql.sh "SELECT count(*) FROM classified_quotes"` | still `97` |
| 5.3 writer lag | `docker compose exec -T kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group results-writer` | `LAG` 0 in every row |
| 5.4 writer catches up | start the app with `APP_PUBLISHER_ENABLED=true`, after ~30 quotes stop it, wait 30 s, start it again | the table reaches `97` with no gap: `SELECT count(DISTINCT quote_id)` is `97` |
| 5.6 database down | with results arriving, rename the table: `bash scripts/neon-sql.sh "ALTER TABLE classified_quotes RENAME TO cq_off"`, wait ~20 s (the log shows the same record failing every 2 s), then rename it back | after the rename back the table has all 97 rows and the log stops failing |
| 6.2 reload | open `http://localhost:8080`, note the counts, press F5 | same counts |
| 6.3 restart replay | restart the app, open `http://localhost:8080/stats` after ~10 s | `total` 97 again |
| 6.4 republish does not grow | after the restart above (the publisher republishes) wait for it to finish | `total` stays 97 |
| 6.5 two tabs | two tabs open during a run, close one | the other keeps updating |
| 6.6 second instance | start a second app with `SERVER_PORT=8081 APP_PUBLISHER_ENABLED=false`; wait ~15 s | both `/stats` (8080 and 8081) show `total` 97; the log of each shows its partitions assigned/revoked |

Expected log lines to look for: `Added`/`Ignored` from the writer, `partitions assigned` on rebalance.

## 2. Block 7 (the app in a container)

1. `powershell scripts/stop-app.ps1` (the IDE app must be stopped), then reset the table and topics as in section 0 (steps 5, 7, 8).
2. `docker compose up --build` (first build takes a few minutes). Expected: `app` starts only after `kafka-init` and `ollama-init` end with code 0.
3. 4.1: `docker run --rm --entrypoint sh kafka-on-kafka-app -c "ls -a /app | grep -c '^.env$'; id -u"` → `0` and a uid that is not `0`. (The image name comes from `docker images`.)
4. 4.3: `http://localhost:8080` updates live; `bash scripts/neon-sql.sh "SELECT count(*) FROM classified_quotes"` → `97`.
5. 4.4: `docker stats --no-stream` while it runs → the `app` MEM USAGE under 640 MiB, and no container `OOMKilled` (`docker inspect -f '{{.State.OOMKilled}}' kafka-on-kafka-app-1` → `false`).
6. 4.5: edit `data/book-config.json` (for example the book name), `docker compose restart app`, check the log shows the new value, no rebuild.

## 3. Block 8: Kafka experiments (to learn)

| Experiment | How | What to observe |
| --- | --- | --- |
| Lag grows and drains | start the app with `PRODUCER_DELAY_MS=500`; run `kafka-consumer-groups.sh --describe --group classifier` every few seconds | `LAG` rises while the model is slower than the publisher, then falls to 0 |
| Partitions and keys | `kafka-console-consumer.sh --topic quotes --from-beginning --property print.partition=true --property print.key=true --max-messages 100` | all of part `II` in partition 1, `I` and `III` in partition 2, partition 0 empty |
| Two instances share the work | second instance as in 6.6, watch `--describe --group classifier` | the three partitions are split between two consumers; stop one and they move back |
| Stop and resume | stop the app mid-run, wait, start it | it continues from the committed offset, nothing is classified twice in the table |
| Another book | change `book-config.json` to another file and restart | new quotes appear; `SELECT book, count(*) ... GROUP BY book` shows both books |

## Failure modes already seen

| Symptom | Cause | Fix |
| --- | --- | --- |
| Port 8080 busy / old behaviour | a leftover app process | `scripts/stop-app.ps1` (checks the port, does not just print "stopped") |
| Topics have 1 partition | a client created them while they were deleted | now blocked by auto-create off and the `kafka-init` check; `scripts/reset-topics.sh` |
| Docker answers 500 | the Docker VM ran out of memory | restart Docker Desktop, close other programs |
| First Ollama call takes minutes | model loading from disk with little free RAM | `ollama-init` loads it once; `OLLAMA_KEEP_ALIVE=-1` keeps it |
| `Connection refused` to Neon | wrong user or password in `.env` | fix `.env` (never paste it in a chat) |
