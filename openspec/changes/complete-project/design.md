## Context

State on `develop` plus the uncommitted work: blocks 1 to 4 are archived and committed; blocks 5, 6 and 7 are written and open as `add-results-writer`, `add-live-feed` and `add-docker-app`. The 47 unit tests passed on the last run. A large part of the final checks was already run by hand against the real Kafka, Ollama and Neon (the full pipeline, 97 rows, feed equal to the database, numbered events, a bad message skipped, lag 0). Three problems surfaced during those runs: a zombie app kept the port, the topics were silently recreated with 1 partition by the broker's auto-creation, and temperature 0 did not make the results fully repeatable. The owner asked for no runs in this task, only code and docs.

## Definition of done for this change

"100%" here means everything that can be finished without running the system:
1. All finished work is committed on this branch.
2. Nothing in the docs claims more than what was measured.
3. The failure modes found while testing are closed in code or config.
4. Every remaining check has a copy-paste recipe with its expected result.
5. A static review found nothing open.

The runs themselves are not part of it and are reported as open.

## Goals / Non-Goals

**Goals:** the five points above, with the smallest possible diff.

**Non-Goals:** any change to Java code or tests, a better classifier (the owner has not chosen; the current model stays and its limits are documented), a cloud LLM provider (decision 21), the Raspberry Pi idea, running anything.

## Decisions

**Topic safety in two places.** The broker gets `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` so no client can ever create a topic with the default of 1 partition. `kafka-init` keeps `--if-not-exists` but then describes each topic and exits with an error when the partition count is not 3 (or `emotions` lacks `retention.ms=-1`), because `--if-not-exists` alone hid exactly the difference that broke the run. Rejected: deleting and recreating on mismatch (it would destroy data silently), and checking only in the docs.

**Scripts only for what went wrong by hand,** three small files in `scripts/`: stop the app and prove the port is free (`stop-app.ps1`; a "stopped" message printed without checking cost a whole run), reset the topics safely (`reset-topics.sh`, which refuses to run while something listens on 8080 and checks the result), and query Neon without printing the password (`neon-sql.sh`, the helper used during the checks). Everything else is a documented command in the runbook, not a script.

**One runbook, `docs/RUNBOOK.md`,** with: preparation, the checks still open (block 5, 6 and 7 items and the Kafka experiments of block 8), the exact commands, the expected result of each and what to do when it differs. It also records what was already verified and how, so nothing is repeated by mistake.

**Docs say what is true.** The package layout stays (renaming folders would touch every file and risk the verified build), but it is described as packages by responsibility inside a Spring Boot app, without ports, because the application classes call the adapter classes directly. Temperature 0 is described as "repeatable in about 95% of the cases (5 of 97 labels differed between two runs)". The classifier quality (about 55% `alienation`, no `bureaucracy`) is listed under known limits and decision 20 stays as the record.

**Tasks of the open changes** are ticked only where the evidence exists from the real runs, with the evidence in the line; the rest stay open and point to the runbook. The three changes are not archived until the runbook has been run.

**Agents.** One independent reviewer reads the branch diff statically (correctness, secrets, doc claims against code). No monitors or shells are needed because nothing runs.

## Risks / Trade-offs

- [A mistake in the stricter `kafka-init` command stops the whole stack from starting] → Keep it a few lines of plain shell, validate the compose file with `docker compose config -q` (parses only, starts nothing) and describe it in the runbook with the expected output.
- [Auto-creation off means a client that starts before `kafka-init` finishes logs warnings and fails] → Compose already orders the app after `kafka-init`; from the IDE the runbook says to run the stack first.
- [Scripts written for Git Bash and PowerShell on Windows may not suit another machine] → Said in the runbook; the commands inside them are standard.
- [Unrun recipes may contain a typo] → Each command is copied from one that already ran during the manual checks, whenever one existed.
