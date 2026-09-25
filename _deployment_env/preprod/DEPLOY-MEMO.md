# PayBridge -- Preprod Deployment Memo

Copy-pasteable checklist for shipping a new build to preprod. It's the
step-by-step distillation of the policy explained in the root `README.md`
("Preprod deploy" section) plus what's encoded in `Dockerfile` and
`deploy.sh` -- read those if you need the *why*, this file is just the *what,
in order*.

Two machines are involved: **local** (where you build/push the image) and
**preprod host** (where you run `deploy.sh`). Each step says which one it's
for.

---

## 0. Before you start -- local

- [ ] Confirm your code changes are committed (and pushed, if this build is
      meant to be reproducible from git history later).
- [ ] Confirm the current live version, so you know what "next" means:
      ```bash
      grep PAYBRIDGE_IMAGE deployment-env/preprod/.env.preprod
      ```
      (run on the preprod host, or check the last tag you pushed to
      `sahelys/paybridge-standalone` on Docker Hub)
- [ ] Decide the next version number -- a plain patch bump from the current
      one (e.g. `1.0.4` -> `1.0.5`). Call this `<NEXT>` for the rest of this
      checklist.

## 1. Build the image -- local, from repo root

```bash
docker build -t sahelys/paybridge-standalone:<NEXT> -t sahelys/paybridge-standalone:latest .
```

One build, two `-t` flags -- both tags land on the exact same image, so they
can never drift apart. Never build/push them as two separate builds.

## 2. Smoke-test the built image before pushing -- local

Don't push an image that's never actually been run. Start it against a real
Postgres and confirm it comes up clean:

```bash
docker run -d --name paybridge-smoketest --network <a-network-with-postgres-reachable> -p 9009:9000 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://<postgres-host>:5432/paybridge" \
  -e SPRING_DATASOURCE_USERNAME=paybridge \
  -e SPRING_DATASOURCE_PASSWORD=<that-postgres's-password> \
  -e PAYBRIDGE_PUBLIC_BASE_URL="http://localhost:9009" \
  sahelys/paybridge-standalone:<NEXT>

curl http://localhost:9009/actuator/health        # expect {"status":"UP"}
docker logs paybridge-smoketest --tail 50         # confirm Flyway migrated cleanly, no stack traces

docker rm -f paybridge-smoketest                  # clean up either way
```

## 3. Push both tags -- local

```bash
docker login
docker push sahelys/paybridge-standalone:<NEXT>
docker push sahelys/paybridge-standalone:latest
```

Never push only one -- anything pinned to `<NEXT>` and anything tracking
`latest` must both resolve to this same build.

## 4. Bump the version in `.env.preprod` -- local (the copy you're about to ship)

Edit `deployment-env/preprod/.env.preprod`:

```
PAYBRIDGE_IMAGE=sahelys/paybridge-standalone:<NEXT>
```

Pin to the exact version, never `latest` -- that's what keeps this specific
deployment reproducible and reversible. Leave every other value as-is unless
this release specifically requires a config change. Never commit this file.

## 5. Ship the two files that actually drive the deploy -- local -> preprod host

Only these two need to reach the preprod host, replacing what's already
there:

- `deployment-env/preprod/compose.preprod.yaml`
- `deployment-env/preprod/.env.preprod` (with the bumped `PAYBRIDGE_IMAGE`
  from step 4)

The host never needs the repository source, `pom.xml`, or `Dockerfile` --
it only ever pulls the pre-built image.

## 6. Run the deploy -- preprod host

From `deployment-env/preprod/` on that host:

```bash
docker login       # only if not already authenticated on this host
./deploy.sh
```

One script does the whole redeploy: stops + removes the currently running
container(s), deletes the image(s) they were using, pulls the new tag,
starts the stack, then polls `/actuator/health` until it's `UP` -- or prints
the app's last 50 log lines and exits non-zero if it never comes up.

## 7. Verify -- preprod host

```bash
curl http://<preprod-host>:9000/actuator/health   # {"status":"UP"}
docker ps                                          # paybridge-standalone and paybridge-postgres both Up
```

Optional, if this release included a migration:

```bash
docker exec paybridge-postgres psql -U paybridge -d paybridge \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

---

## Rollback

If step 7 fails, or the new version misbehaves once live: edit
`.env.preprod` on the preprod host back to the previous known-good tag
(`PAYBRIDGE_IMAGE=sahelys/paybridge-standalone:<PREVIOUS>`) and rerun
`./deploy.sh`. The previous image is still on Docker Hub, so this is a
straight redeploy, not a rebuild.
