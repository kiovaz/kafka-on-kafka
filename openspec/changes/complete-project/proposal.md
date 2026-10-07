## Why

The pipeline works end to end (the last real run classified 97 quotes, wrote 97 rows to Neon and showed 97 in the live feed), but the project is not finished: three blocks are written and uncommitted, the docs still over-claim a "hexagonal" architecture and a determinism the measurements did not confirm, a silent topic-creation race hurt the last verification, and the remaining checks and Kafka experiments have no repeatable recipe. This change closes everything that can be closed by writing code and docs, and leaves a kit to run the rest in one go.

## What Changes

- Commit the three finished blocks (results writer, live feed, Docker app) on this branch, in separate commits.
- **Topics can no longer be created by accident:** the broker stops auto-creating topics, and `kafka-init` fails loudly when an existing topic does not have the expected partitions or retention (today `--if-not-exists` hides the difference).
- **Honest docs:** drop the "hexagonal architecture" claim (the code is a Spring Boot app with packages by responsibility and no ports), correct the claim that temperature 0 makes results repeatable (measured: about 95%), record the topic-creation finding, and state what is verified and what is not.
- **A runnable kit instead of ad-hoc commands:** small scripts for the things that went wrong by hand (stop the app and prove the port is free, reset the topics safely, query Neon without printing the password) and one runbook with every remaining check and the block 8 Kafka experiments, with the expected result of each.
- Close the loop in OpenSpec and `TODO.md`: tick what was really verified (with the evidence), keep the rest open.
- An independent static review of the branch before the final commit.
- No Java behaviour changes. Nothing is run against Docker, Kafka, Ollama or Neon in this change.

## Capabilities

### New Capabilities

### Modified Capabilities
- `local-infrastructure`: topics are created only by `kafka-init`, and a wrong existing topic is an error.

## Impact

- `docker-compose.yml` (broker setting, stricter `kafka-init`), `README.md`, `docs/DECISIONS.md`, `TODO.md`, new `scripts/` and `docs/RUNBOOK.md`, and the task lists of the three open changes.
- No source or test files change, so the 47 tests that passed keep their meaning.
- What stays open on purpose: the runs listed in the runbook (restart, second instance, the Docker app, DB outage), and the owner's actions (reset the Neon password, restart Docker if it falls).
