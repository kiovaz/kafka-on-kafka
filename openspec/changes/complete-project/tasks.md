## 1. Branch and commits

- [x] 1.1 On the branch `autonomo/finalizar-projeto`, commit the results writer (code, tests, `pom.xml`, `application.yml`, `schema.sql` and its OpenSpec change) as one commit
- [x] 1.2 Commit the live feed (code, page, tests and its OpenSpec change) as one commit
- [x] 1.3 Commit the Docker app (`Dockerfile`, `.dockerignore`, the `app` service and its OpenSpec change) as one commit

## 2. Topic safety

- [x] 2.1 Make `kafka-init` describe each topic after creating it and exit with an error naming the topic when `quotes` or `emotions` does not have 3 partitions or `emotions` lacks `retention.ms=-1`, keep `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` on the broker, and validate the file with `docker compose config -q` (parse only)

## 3. Honest documentation

- [x] 3.1 README: remove the hexagonal claim (overview, scope, layout section title and text, design decisions) and describe the packages by responsibility; correct the determinism sentence; add a "Known limits" section (classifier bias, replay growth, Neon wake-up, memory) and a "Status" section that says what is verified and what is not
- [x] 3.2 `docs/DECISIONS.md`: retitle decision 2 (no hexagonal claim), correct decision 20 (about 95% repeatable, measured), add decision 25 (topics are created only by `kafka-init`; what happened) and decision 26 (what "done" means for this project)
- [x] 3.3 Update `TODO.md` to the real state

## 4. Run kit

- [x] 4.1 Add `scripts/stop-app.ps1`, `scripts/reset-topics.sh` and `scripts/neon-sql.sh`, each reading nothing it should not print
- [x] 4.2 Add `docs/RUNBOOK.md` with the preparation, the open checks of blocks 5, 6 and 7 and the experiments of block 8 (commands and expected results), the failure modes seen and what was already verified with its evidence

## 5. OpenSpec housekeeping

- [ ] 5.1 Update the task lists of `add-results-writer`, `add-live-feed` and `add-docker-app`: tick what was really verified, with the evidence in the line, and point the open ones to the runbook
- [x] 5.2 Run `openspec validate` on this change and on the three open ones

## 6. Review

- [ ] 6.1 Have an independent agent review the branch diff statically (correctness, secrets, claims in the docs against the code) and report findings
- [ ] 6.2 Fix every finding that holds, or record why not

## 7. Finish

- [ ] 7.1 Commit the rest in clear commits, confirm the working tree is clean and no file tracked by Git contains a secret, and write the final report listing exactly what remains to be run
