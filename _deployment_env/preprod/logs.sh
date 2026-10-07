#!/usr/bin/env bash
# Manual debugging tool -- NOT part of the Jenkins pipeline.
# Follows ONE container's logs, by container name: independent of compose,
# .env.preprod and VERSION, it only needs Docker and the container running.
#
# Usage:
#   ./logs.sh             # asks which container
#   ./logs.sh backend | frontend | db

set -euo pipefail

# Fixed container_name values from compose.preprod.yaml.
BACKEND=paybridge-standalone-back
FRONTEND=paybridge-standalone-front
DB=paybridge-standalone-postgres

TARGET="${1:-}"

if [ -z "$TARGET" ]; then
  echo "What do you want to log?"
  echo "  1) backend  ($BACKEND)"
  echo "  2) frontend ($FRONTEND)"
  echo "  3) db       ($DB)"
  read -rp "Choice [1/2/3]: " CHOICE
  case "$CHOICE" in
    1) TARGET=backend ;;
    2) TARGET=frontend ;;
    3) TARGET=db ;;
    *) echo "Unrecognized choice '$CHOICE'." >&2; exit 1 ;;
  esac
fi

case "$TARGET" in
  backend)  CONTAINER="$BACKEND" ;;
  frontend) CONTAINER="$FRONTEND" ;;
  db)       CONTAINER="$DB" ;;
  *) echo "Unknown target '$TARGET' -- use backend | frontend | db." >&2; exit 1 ;;
esac

# -f: keep streaming new lines (like `tail -f`); --tail=200: start with the last
# 200 lines of history for context instead of the whole log.
exec docker logs -f --tail=200 "$CONTAINER"
