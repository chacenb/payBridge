#!/usr/bin/env bash
# Deploy/redeploy PayBridge on this host from the pre-built images
# <repository from .env.preprod>:<version from the VERSION file next to this script (shipped by CI from the backend repo root)> --
# backend and frontend share that one version.
# Run this script from anywhere -- it always resolves compose.preprod.yaml, .env.preprod and VERSION next to itself, not the working directory.
# Never builds anything; only pulls and (re)starts.
# Manual rollback: ./deploy.sh x.y.z

# Prerequisite (only if the registry is private):
# `docker login` on this host with an account that can pull the image.
# Not done automatically here so no credential ever needs to live in this script.

# -e: exit immediately if any command fails.
# -u: treat use of an unset variable as an error, instead of silently expanding to an empty string.
# -o pipefail: a pipeline (e.g. `grep | cut`) fails if ANY stage fails, not just the last one.
set -euo pipefail

# Resolve paths relative to THIS script's own location, not the caller's current directory --
# so the deploy folder can live anywhere and still work with a plain `./deploy.sh`.
# NOTE: this expects the FLAT layout used on the host -- deploy.sh, compose.preprod.yaml,
# .env.preprod and VERSION all in one folder. In the repo they are spread out
# (_deployment_env/, _deployment_env/preprod/, repo root), so run it on the host, not from a checkout.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/compose.preprod.yaml"
ENV_FILE="$SCRIPT_DIR/.env.preprod"

# The single source of truth 'VERSION' file (lives at the backend repo root, shipped next to this script by CI)
# For a manual rollback, pass a version as the first argument to this script, e.g. `./deploy.sh 1.2.3`.
PAYBRIDGE_ROLLBACK_VERSION="${1:-}"
PAYBRIDGE_VERSION="${PAYBRIDGE_ROLLBACK_VERSION:-$(tr -d '[:space:]' < "$SCRIPT_DIR/VERSION")}"
export PAYBRIDGE_VERSION
echo "==> Deploying version ${PAYBRIDGE_VERSION}"

# Check the presence of the .env file before doing anything else
if [ ! -f "$ENV_FILE" ]; then
  echo "Error: $ENV_FILE not found." >&2
  exit 1
fi

# Host folder the back's rolling logs are bind-mounted to (see compose.preprod.yaml).
# Prepared HERE, before anything is stopped: if Docker created the missing folder itself it
# would be root-owned and the container (running as PAYBRIDGE_UID:PAYBRIDGE_GID) could not write to it.
env_value() { grep -E "^$1=" "$ENV_FILE" | tail -1 | cut -d= -f2- | tr -d '"' || true; }
LOGS_DIR="$(env_value PAYBRIDGE_LOGS_DIR)"
LOGS_DIR="${LOGS_DIR:-/mnt/PAYBRIDGELUN/LOGS}"
LOGS_UID="$(env_value PAYBRIDGE_UID)"
LOGS_GID="$(env_value PAYBRIDGE_GID)"

echo "==> Preparing log folder ${LOGS_DIR}"
mkdir -p "$LOGS_DIR"
# As root (the pipeline runs `sudo ./deploy.sh`) hand the folder to the user the container runs as.
if [ "$(id -u)" -eq 0 ] && [ -n "$LOGS_UID" ] && [ -n "$LOGS_GID" ]; then
  chown "$LOGS_UID:$LOGS_GID" "$LOGS_DIR"
fi
if [ ! -w "$LOGS_DIR" ] && [ "$(id -u)" -ne 0 ]; then
  echo "Error: $LOGS_DIR is not writable by $(id -un) -- fix its ownership or run with sudo." >&2
  exit 1
fi

# Small wrapper so every docker compose call below always targets the right compose file and env file without repeating both flags each time.
compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

# Snapshot the image(s) the current container(s) are running BEFORE touching anything --
# once they're stopped/removed there's no other way to know what
# to delete. Empty on a first-ever run (nothing deployed yet), which is fine.
OLD_IMAGE_IDS="$(compose ps -q | xargs -r docker inspect -f '{{.Image}}' 2>/dev/null | sort -u)"

# Stop and remove the running container(s) for this stack first -- a running
# container holds a reference to its image, so the image can't be deleted
# while it's still up. `down` is safe to run even if nothing is running.
echo "-------------------------------------------"
echo "==> Stopping and removing current container(s)"
compose down

# Delete the image(s) that were just replaced, now that nothing references
# them. Best-effort (`|| true`): e.g. the same tag pulled again would already
# have moved, or the image might still be shared with another running stack.
if [ -n "$OLD_IMAGE_IDS" ]; then
  echo "-------------------------------------------"
  echo "==> Removing previous image(s)"
  echo "$OLD_IMAGE_IDS" | xargs -r docker rmi || true
fi

# Pulling images explicitly here
echo "-------------------------------------------"
echo "==> Pulling images"
compose pull

# Recreates any service whose image/config changed; leaves the rest alone.
echo "-------------------------------------------"
echo "==> Starting stack"
compose up -d

# Don't just trust "container started"
# Poll each service's own signal of life so this script only reports success once the whole stack is actually serving traffic, not just running.
echo "-------------------------------------------"
echo "==> Waiting for the backend to report healthy within ~1 minute"
PORT="$(grep -E '^PAYBRIDGE_PORT=' "$ENV_FILE" | tail -1 | cut -d= -f2)"
PORT="${PORT:-9000}"

BACKEND_UP=false
# Up to 30 tries, 2s apart (~1 minute total) before giving up.
for _ in $(seq 1 30); do
  if curl -s -o /dev/null -w '%{http_code}' "http://localhost:${PORT}/actuator/health" 2>/dev/null | grep -q '^200$'; then
    BACKEND_UP=true
    break
  fi
  sleep 2
done

if [ "$BACKEND_UP" != true ]; then
  echo "Backend did not report healthy in time -- recent logs:" >&2
  compose logs --tail=100 paybridge >&2
  exit 1
fi
echo "==> Backend healthy on port ${PORT}"

# The frontend has no /actuator-style health endpoint -- a plain 200 on its
# /paybridge/ context is enough to confirm nginx came up and is serving the
# SPA. (Not "/": nginx redirects it to /paybridge/ with a 302, never a 200.)
echo "-------------------------------------------"
echo "==> Waiting for the frontend to respond within ~1 minute"
FRONT_PORT="$(grep -E '^PAYBRIDGE_FRONT_PORT=' "$ENV_FILE" | tail -1 | cut -d= -f2)"
FRONT_PORT="${FRONT_PORT:-9001}"

FRONTEND_UP=false
for _ in $(seq 1 30); do
  if curl -s -o /dev/null -w '%{http_code}' "http://localhost:${FRONT_PORT}/paybridge/" 2>/dev/null | grep -q '^200$'; then
    FRONTEND_UP=true
    break
  fi
  sleep 2
done

if [ "$FRONTEND_UP" != true ]; then
  echo "Frontend did not respond in time -- recent logs:" >&2
  compose logs --tail=100 paybridge-front >&2
  exit 1
fi

echo "-------------------------------------------"
echo "==> Healthy: backend on port ${PORT}, frontend on port ${FRONT_PORT}"
exit 0
