# PayBridge

PayBridge is a standalone Spring Boot service for mobile-money operator integrations. The current deployment baseline starts the service and PostgreSQL; the callback-inbox endpoint is the next implementation slice.

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

The service listens on port `9000` by default, matching the existing Moov callback configuration. The callback route to be implemented is `/sahelyspay/payment/callback`.

## Configuration

All runtime configuration is supplied through environment variables. `.env.example` documents the local variables; never commit `.env`, deployment profiles, or the raw `MATERIALS` directory.

The Moov variables are passed through Docker Compose now so the later payment adapter can consume them. `MOOV_TLS_VERIFY=false` is retained as a temporary compatibility setting for the initial integration, matching the existing `curl -k` scripts.

## Current stack state

- Spring Boot service: Dockerized and exposes Actuator health.
- PostgreSQL: connected through Spring Data JPA and retained in a persistent named volume. It is reachable only from the Docker network; the database port is not published to the host.
- Callback persistence and callback HTTP route: next implementation step.
