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

1. Build and push both tags -- see the versioning policy above for the exact
   commands.

2. On the preprod host, copy only two files there: `compose.preprod.yaml` and a
   `.env.preprod` you write from `.env.preprod.example`, filling in every
   `REPLACE_ME_*` value (a fresh `POSTGRES_PASSWORD`, preprod's own Moov
   credentials, `PAYBRIDGE_IMAGE=sahelys/paybridge-standalone:<next-version>`
   (pin to the specific version, not `latest`, so this deployment stays
   reproducible), and a
   `MOMO_RESULT_BASE_URL` pointing at this host's own public address, base
   only -- the callback path is appended automatically from
   `PAYBRIDGE_CALLBACK_PATH`). Never commit
   `.env.preprod`.

3. Pull and start, always passing `--env-file` explicitly -- `-f` alone does not
   select a matching env file, Compose otherwise only ever looks for a file
   literally named `.env`:

   ```bash
   docker login
   docker compose --env-file .env.preprod -f compose.preprod.yaml pull
   docker compose --env-file .env.preprod -f compose.preprod.yaml up -d
   ```

4. Verify the same way as local dev: `curl http://<host>:9000/actuator/health`.

To ship a new version later: repeat step 1 (both tags, both pushed), update
`PAYBRIDGE_IMAGE` in `.env.preprod` to the new version, then repeat step 3's
`pull`/`up -d`.

## Configuration

All runtime configuration is supplied through environment variables. `.env.example` documents the local variables; never commit `.env`, deployment profiles, or the raw `MATERIALS` directory.

The Moov variables are passed through Docker Compose now so the later payment adapter can consume them. `MOOV_TLS_VERIFY=false` is retained as a temporary compatibility setting for the initial integration, matching the existing `curl -k` scripts.

## Current stack state

- Spring Boot service: Dockerized and exposes Actuator health.
- PostgreSQL: connected through Spring Data JPA and retained in a persistent named volume. Its port is published to the host (`POSTGRES_PORT`, default `5432`) for local development inspection only -- do not publish it in any shared or production environment.
- Callback persistence and callback HTTP route: implemented, store-only (see above).
