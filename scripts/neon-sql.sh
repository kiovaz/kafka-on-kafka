#!/bin/bash
# usage: scripts/neon-sql.sh "SELECT count(*) FROM classified_quotes"
# Runs one SQL statement on Neon through psql in a container. DB_URL, DB_USER and DB_PASSWORD come from the
# git-ignored .env and are never printed.
cd "$(dirname "$0")/.." || exit 1
url=$(grep '^DB_URL=' .env | cut -d= -f2-)
export PGHOST=$(echo "$url" | sed -E 's#jdbc:postgresql://([^/]+)/.*#\1#')
export PGDATABASE=$(echo "$url" | sed -E 's#jdbc:postgresql://[^/]+/([^?]+).*#\1#')
export PGUSER=$(grep '^DB_USER=' .env | cut -d= -f2-)
export PGPASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d= -f2-)
MSYS_NO_PATHCONV=1 docker run --rm -e PGHOST -e PGDATABASE -e PGUSER -e PGPASSWORD -e PGSSLMODE=require -e PGCHANNELBINDING=require postgres:16-alpine psql -X -q -t -A -F ' | ' -c "$1"
