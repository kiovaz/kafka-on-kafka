#!/bin/bash
# Deletes quotes and emotions and recreates them through kafka-init, then checks 3 partitions and retention.
# Run it with the app stopped (scripts/stop-app.ps1): a running client would be disturbed.
# The data in both topics is lost. The consumer offsets of the groups are not touched by this script;
# kafka-consumer-groups --delete removes them (the runbook says when).
cd "$(dirname "$0")/.." || exit 1
if node -e "require('net').connect(8080,'localhost').on('connect',()=>process.exit(0)).on('error',()=>process.exit(1))"; then
  echo "ERROR: something listens on 8080 (the app?). Stop it first: powershell scripts/stop-app.ps1"; exit 1
fi
K="docker compose exec -T kafka /opt/kafka/bin"
for t in quotes emotions; do $K/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic $t 2>&1 | grep -v -i "does not exist"; done
sleep 3
docker compose up kafka-init 2>&1 | tail -5
for t in quotes emotions; do
  $K/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic $t | head -1
done
