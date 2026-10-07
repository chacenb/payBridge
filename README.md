# PayBridge

PayBridge is a standalone Spring Boot service for mobile-money operator integrations. The current deployment baseline starts the service and PostgreSQL and durably receives Moov Money's async callback POST.

## Local Docker startup

1. Start Docker Desktop.
2. Copy `.env.example` to `.env` and set a strong local `POSTGRES_PASSWORD`.
3. Start the stack:

   ```bash
   docker compose up --build
   ```

4. Check the service is ready:

   ```bash
   curl http://localhost:9000/actuator/health
   ```

The service listens on port `9000` by default, matching the existing Moov callback configuration. The callback route is `/sahelyspay/payment/callback` -- it durably persists every received POST (raw body, headers, hash) into `operator_callbacks` and returns `200 OK`; it does not yet correlate a callback to a payment or interpret its outcome.

## Preprod deploy

Preprod runs from a pre-built image only -- the host never needs this repository's
source, `pom.xml`, or `Dockerfile`.

---

### :warning: MEMO -- versioning policy for every rebuild

**Every image rebuild after a code update MUST produce and push *both* the next
version tag AND `latest` to Sahelys' Docker Hub -- never push only one.**

- The version tag lets any deployment stay pinned to one exact, reproducible
  image, and be rolled back to a prior one on demand.
- `latest` lets anything that intentionally tracks the newest build get it.
- Building with two `-t` flags builds the image **once** and applies both tags
  to the identical result, so they can never drift apart from each other.

Worked example, going from `1.0.2` to `1.0.3`:

```bash
docker build -t sahelys/paybridge-standalone:1.0.3 -t sahelys/paybridge-standalone:latest .
docker login
docker push sahelys/paybridge-standalone:1.0.3
docker push sahelys/paybridge-standalone:latest
```

---

The release number lives in one place: the `VERSION` file at the root of this
repository. Backend and frontend images share it, and `deploy.sh` on the host
reads the copy shipped next to it. Normally Jenkins does all of the below -- see
`_deployment_env/preprod/DEPLOY-MEMO.md`. The manual flow is the fallback.

1. Bump `VERSION`, then build and push both tags of **both** images (backend
   here, frontend from its own repo) -- see the versioning policy above for the
   exact commands.

2. On the preprod host, the deploy folder holds: `compose.preprod.yaml` and
   `VERSION` (copy these each release), `deploy.sh` (copy once, `chmod +x`), and
   a `.env.preprod` you write from `.env.preprod.example`, filling in every
   `REPLACE_ME_*` value (a fresh `POSTGRES_PASSWORD`, preprod's own Moov
   credentials, and a `MOMO_RESULT_BASE_URL` pointing at this host's own public
   address, base only -- the callback path is appended automatically from
   `PAYBRIDGE_CALLBACK_PATH`). `PAYBRIDGE_IMAGE` and `PAYBRIDGE_FRONT_IMAGE` are
   repository names only, **no `:tag`** -- the tag comes from `VERSION`. Never
   commit `.env.preprod`.

3. Deploy from that folder. `deploy.sh` pins the images to `VERSION`, stops the
   old stack, pulls, starts, and waits for the health checks:

   ```bash
   docker login   # once, only if the registry is private
   ./deploy.sh
   ```

4. Verify: `curl http://<host>:9000/actuator/health` (backend) and
   `curl -I http://<host>:9001/paybridge/` (frontend).

To ship a new version later: repeat step 1, copy the new `compose.preprod.yaml`
and `VERSION` to the host, then run `./deploy.sh` again. To roll back, run
`./deploy.sh <older-version>`.

## Configuration

All runtime configuration is supplied through environment variables. `.env.example` documents the local variables; never commit `.env`, deployment profiles, or the raw `MATERIALS` directory.

The Moov variables are passed through Docker Compose now so the later payment adapter can consume them. `MOOV_TLS_VERIFY=false` is retained as a temporary compatibility setting for the initial integration, matching the existing `curl -k` scripts.

## Current stack state

- Spring Boot service: Dockerized and exposes Actuator health.
- PostgreSQL: connected through Spring Data JPA and retained in a persistent named volume. Its port is published to the host (`POSTGRES_PORT`, default `5432`) for local development inspection only -- do not publish it in any shared or production environment.
- Callback persistence and callback HTTP route: implemented, store-only (see above).
