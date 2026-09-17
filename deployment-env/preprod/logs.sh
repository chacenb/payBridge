#!/usr/bin/env bash
# Follow PayBridge's (and Postgres's) container logs live on this host.
# Run from anywhere -- resolves compose.preprod.yaml and .env.preprod next
# to itself, same as deploy.sh.
#
# Usage:
#   ./logs.sh             # asks interactively what to follow
#   ./logs.sh paybridge   # follow just the app, no prompt
#   ./logs.sh postgres    # follow just the database, no prompt
#   ./logs.sh backend     # alias for paybridge
#   ./logs.sh db          # alias for postgres

set -euo pipefail

# Resolve paths relative to THIS script's own location, not the caller's
# current directory -- same reasoning as deploy.sh.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/compose.preprod.yaml"
ENV_FILE="$SCRIPT_DIR/.env.preprod"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: $ENV_FILE not found." >&2
  echo "Copy .env.preprod.example to .env.preprod in this folder and fill in every REPLACE_ME_* value first." >&2
  exit 1
fi

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

# Only prompt when no argument was given -- passing one explicitly (e.g. from
# another script, or a habit already muscle-memorized) always skips the menu.
SERVICE="${1:-}"

if [ -z "$SERVICE" ]; then
  echo "What do you want to log?"
  echo "  1) backend (paybridge)"
  echo "  2) DB (postgres)"
  read -rp "Choice [1/2, Enter = both]: " CHOICE
  case "$CHOICE" in
    1) SERVICE="paybridge" ;;
    2) SERVICE="postgres" ;;
    "") SERVICE="" ;;          # Enter/blank -- both, no filter
    *) echo "Unrecognized choice '$CHOICE', following both." >&2; SERVICE="" ;;
  esac
else
  # Accept friendly aliases alongside the real compose service names.
  case "$SERVICE" in
    backend) SERVICE="paybridge" ;;
    db) SERVICE="postgres" ;;
  esac
fi

# -f/--follow: keep streaming new lines as they're written, instead of
# printing what exists so far and exiting (like `tail -f`).
# --tail=200: start with the last 200 lines of history so you have context,
# then keep following from there -- not the entire log since the container
# started.
# $SERVICE left unquoted on purpose: when empty (both/default), it must
# disappear entirely rather than pass an empty-string argument to docker
# compose logs, which would error.
compose logs -f --tail=200 $SERVICE
