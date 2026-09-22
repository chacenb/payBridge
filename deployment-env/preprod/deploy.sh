#!/usr/bin/env bash
# Deploy/redeploy PayBridge on this host from the pre-built image named in
# .env.preprod. Run this script from anywhere -- it always resolves
# compose.preprod.yaml and .env.preprod next to itself, not the working
# directory. Never builds anything; only pulls and (re)starts.
#
# Prerequisite (only if the registry is private): `docker login` on this host
# with an account that can pull the image. Not done automatically here so no
# credential ever needs to live in this script.

# -e: exit immediately if any command fails (a failed pull must never fall
#     through to "up -d" against a stack that never actually got the new image).
# -u: treat use of an unset variable as an error, instead of silently
#     expanding to an empty string.
# -o pipefail: a pipeline (e.g. `grep | cut`) fails if ANY stage fails, not
#     just the last one.
set -euo pipefail

# Resolve paths relative to THIS script's own location, not the caller's
# current directory -- so the whole deployment-env/preprod/ folder can be
# copied anywhere and still work with a plain `./deploy.sh`.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/compose.preprod.yaml"
ENV_FILE="$SCRIPT_DIR/.env.preprod"

# Refuse to run against a folder that only has the .example template --
# that would mean pulling REPLACE_ME_registry/paybridge:REPLACE_ME_tag and
# failing confusingly deep inside docker compose instead of here.
if [ ! -f "$ENV_FILE" ]; then
  echo "Error: $ENV_FILE not found." >&2
  echo "Copy .env.preprod.example to .env.preprod in this folder and fill in every REPLACE_ME_* value first." >&2
  exit 1
fi

# Small wrapper so every docker compose call below always targets the right
# compose file and env file without repeating both flags each time.
compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

# Snapshot the image(s) the current container(s) are running BEFORE touching
# anything -- once they're stopped/removed there's no other way to know what
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

# Pull BEFORE up, on purpose: `up -d` alone only pulls an image if nothing
# with that tag exists locally yet, so redeploying the same tag with new
# content would silently reuse the stale local copy. Pulling explicitly here
# also fails fast (bad tag, auth, network) before anything currently running
# is touched.
echo "-------------------------------------------"
echo "==> Pulling images"
compose pull

# Recreates any service whose image/config changed; leaves the rest alone.
echo "-------------------------------------------"
echo "==> Starting stack"
compose up -d

# Don't just trust "container started" -- poll the app's own health endpoint
# so this script only reports success once it's actually serving traffic.
echo "-------------------------------------------"
echo "==> Waiting for the app to report healthy"
PORT="$(grep -E '^PAYBRIDGE_PORT=' "$ENV_FILE" | tail -1 | cut -d= -f2)"
PORT="${PORT:-9000}"

# Up to 30 tries, 2s apart (~1 minute total) before giving up.
for _ in $(seq 1 30); do
  if curl -s -o /dev/null -w '%{http_code}' "http://localhost:${PORT}/actuator/health" 2>/dev/null | grep -q '^200$'; then
    echo "-------------------------------------------"
    echo "==> Healthy on port ${PORT}"
    exit 0
  fi
  sleep 2
done

# Never became healthy in time -- surface the app's own recent logs instead
# of leaving you to go dig them up by hand right after a failed deploy.
echo "App did not report healthy in time -- recent logs:" >&2
compose logs --tail=50 paybridge >&2
exit 1
