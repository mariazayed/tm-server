#!/usr/bin/env bash
# Dumps the investing tracker's tables (invest_*) from PostgreSQL to a compressed SQL file and keeps
# the newest few. Connection settings come from the standard libpq variables (PGHOST, PGPORT,
# PGUSER, PGDATABASE, PGPASSWORD or ~/.pgpass). Run it from cron, for example nightly:
#
#   30 2 * * *  PGDATABASE=tm_db BACKUP_DIR=/var/backups/invest /path/to/backup-invest-db.sh
#
# Restore into an empty database that already has the schema (run the app once so Flyway
# creates it), after clearing the invest tables:
#
#   gunzip -c invest-db-YYYYMMDD-HHMMSS.sql.gz | psql -d tm_db
#
# Optional: BACKUP_DIR (default ./db-backups), KEEP (default 14).
set -euo pipefail

: "${PGDATABASE:?Set PGDATABASE to the database name}"
BACKUP_DIR="${BACKUP_DIR:-./db-backups}"
KEEP="${KEEP:-14}"

case "$KEEP" in
  ''|*[!0-9]*|0) echo "KEEP must be a positive whole number" >&2; exit 2 ;;
esac

umask 077 # dumps contain your notes and reminder address: owner-only
mkdir -p "$BACKUP_DIR"

stamp="$(date -u +%Y%m%d-%H%M%S)"
final="$BACKUP_DIR/invest-db-$stamp.sql.gz"
tmp="$(mktemp "$BACKUP_DIR/.dump-XXXXXX")"
trap 'rm -f "$tmp"' EXIT

# pipefail makes a pg_dump failure fail the whole pipeline, so a broken dump is never kept.
pg_dump --no-owner --no-privileges --clean --if-exists -t 'invest_*' -T invest_ai_usage | gzip > "$tmp"
gzip -t "$tmp"                      # the archive must be readable
[ -s "$tmp" ] || { echo "dump is empty" >&2; exit 1; }
mv "$tmp" "$final"
trap - EXIT

# Retention: names sort chronologically.
count=0
for f in $(ls -1 "$BACKUP_DIR"/invest-db-*.sql.gz 2>/dev/null | sort -r); do
  count=$((count + 1))
  if [ "$count" -gt "$KEEP" ]; then rm -f -- "$f"; fi
done

echo "Wrote $final"
