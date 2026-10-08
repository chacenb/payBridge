# PayBridge -- Preprod Deployment Memo

How a release reaches preprod. The version is specified **once**, in the
`VERSION` file at the backend repo root (shipped next to `compose.preprod.yaml` on the server); backend and frontend images share it.

```
VERSION  -->  Jenkinsfile  -->  build backend + frontend images
                            -->  push <VERSION> and latest (both images)
                            -->  ssh preprod host -->  ./deploy.sh
```

## Release (normal flow)

1. Bump `VERSION` (backend repo root) (plain patch bump, e.g. `1.0.16` -> `1.0.17`).
2. Commit and push it to the backend repo (together with any code changes).
3. Run the Jenkins job (`BACKEND_BRANCH` / `FRONTEND_BRANCH` default to `develop`).

The job does the following : 
1. clones both repos, 
2. Builds both images tagged `<VERSION>` + `latest`,
3. Pushes all four tags, 
4. Copies only `compose.preprod.yaml` and `VERSION` to the destination server
(`deploy.sh` and `logs.sh` already live there -- see One-time setup), 
5. Then simply runs the server's own `./deploy.sh`. 
   1. The `deploy.sh` script stops the old stack, 
   2. removes the replaced images, 
   3. pulls the pinned images, 
   4. starts the stack 
   5. And polls backend (`/actuator/health`) and frontend
(`/paybridge/`)

> IMPORTANT : The secret file `.env.preprod` is never commited nor shipped. 
> If any modification is done, shipt it manually to the server.

## Repo layout vs server layout

The repo is organised by role, the server folder is flat:

| Repo | Server (`PREPROD_HOST_DIR`) | Who puts it there |
|---|---|---|
| `VERSION` (repo root) | `VERSION` | Jenkins, every release |
| `deployment-environment/preprod/compose.preprod.yaml` | `compose.preprod.yaml` | Jenkins, every release |
| `deployment-environment/deploy.sh` | `deploy.sh` | by hand, once |
| `deployment-environment/logs.sh` | `logs.sh` | by hand, once (optional) |
| `deployment-environment/preprod/.env.preprod.example` | `.env.preprod` | by hand (secret, never committed) |

`deploy.sh` resolves everything next to itself, so it only works in the flat server layout.

## Server prerequisites (one-time, by hand)

The scripts **assume all of this exists** and never create it. `deploy.sh` runs a
read-only preflight and stops with the fix to apply if something is missing.

| # | Element | Command | Check |
|---|---|---|---|
| 1 | Docker Engine + compose plugin | install per distro | `docker compose version` |
| 2 | Docker starts at boot | `sudo systemctl enable --now docker` | `systemctl is-enabled docker` |
| 3 | Deploy user (`rocky`) can run Docker | `sudo usermod -aG docker rocky`, log in again | `docker ps` as `rocky`, no sudo |
| 4 | Jenkins SSH key authorized for the deploy user | add the public key to `~rocky/.ssh/authorized_keys` | SSH from the agent works |
| 5 | App user (owns the logs) | `sudo useradd --system --no-create-home --shell /usr/sbin/nologin --uid 10001 paybridge` | `id paybridge` shows `10001` |
| 6 | LUN mounted | add to `/etc/fstab`, `mount` | `findmnt /mnt/PAYBRIDGELUN` |
| 7 | Logs folder | `sudo mkdir -p /mnt/PAYBRIDGELUN/LOGS && sudo chown paybridge:paybridge /mnt/PAYBRIDGELUN/LOGS && sudo chmod 755 /mnt/PAYBRIDGELUN/LOGS` | `ls -ld /mnt/PAYBRIDGELUN/LOGS` |
| 8 | Deploy folder (`PREPROD_HOST_DIR`) | `mkdir -p /home/rocky/payBridgeStandalone` as `rocky` | `ls -ld` shows owner `rocky` |
| 9 | `deploy.sh` (and optional `logs.sh`) | copy by hand, `chmod +x` -- the pipeline never overwrites them, so re-copy after any change | `ls -l` |
| 10 | `.env.preprod` | copy from `.env.preprod.example`, fill it, `chmod 600` | `ls -l` |
| 11 | Registry login (private registry only) | `docker login` as `rocky` | `docker pull <image>:latest` |
| 12 | Network | open ports 9000 and 9001; Moov must reach `MOMO_RESULT_BASE_URL` | `curl` from outside |

uid `10001` must match the non-root user of the image (Dockerfile).

**Jenkins agent (once)**: Docker CLI; plugins Git, Credentials Binding, SSH Agent;
credentials for GitLab (read), Docker Hub (push) and the SSH key of the deploy
user; the server's host key pre-trusted, since the pipeline uses
`StrictHostKeyChecking=yes`:

```bash
ssh-keyscan <preprod-host> >> ~jenkins/.ssh/known_hosts
```

**Done by Docker, not by the scripts** (declared in `compose.preprod.yaml` / the
image): the `postgres-data` volume and the default network on the first `up`;
Postgres creates its database and user from `POSTGRES_*` on the **first start
only** (changing the password later in `.env.preprod` does not change the stored
one); Flyway runs the migrations when the app starts; `restart: unless-stopped`
brings the containers back after a reboot (needs #2).

In `.env.preprod`, `PAYBRIDGE_IMAGE` and `PAYBRIDGE_FRONT_IMAGE` are **repository
only, no `:tag`** (the tag comes from `VERSION`). Fill the `REPLACE_ME_*` values and
credential IDs at the top of the `Jenkinsfile`.

## Verify

```bash
curl http://<preprod-host>:9000/actuator/health      # {"status":"UP"}
curl -I http://<preprod-host>:9001/paybridge/        # 200
docker ps                                             # paybridge-standalone-back/-front/-postgres Up
```

## Logs

### Application log files (host)

The back writes a rolling log file in addition to the console output. It is
**bind-mounted to the host** so it can be read from the server's own OS and
survives every release (`compose down` never touches it):

| What | Where / how |
|---|---|
| Host folder | `PAYBRIDGE_LOGS_DIR` in `.env.preprod` (default `/mnt/PAYBRIDGELUN/LOGS`) |
| Live file | `<folder>/paybridge.log` (`LOGFILE_NAME`) |
| Rotation | daily, at every app start, and past `LOG_FILE_SIZE`; archives are `paybridge.log_<yyyyMMdd>-<n>.log.gz` |
| Cleanup | rotated archives older than `DELETE_LOGS_OLDER_THAN_X_DAYS` are deleted -- only those, nothing else in the folder |
| Level | `APP_LOG_LEVEL` (restart to change; no rebuild) |
| Owner | `paybridge` (uid `10001`, the image's non-root user); files are world-readable, deleting them by hand needs `sudo` |

The folder is a server prerequisite (#7): `deploy.sh` does not create it, it checks it
(exists, owned by uid `10001`) before stopping anything. `/mnt/PAYBRIDGELUN` itself
must be mounted (#6).

```bash
tail -f /mnt/PAYBRIDGELUN/LOGS/paybridge.log
```

### logs.sh

`logs.sh` is a manual debugging tool, **not part of the pipeline** -- Jenkins
never ships or runs it. Copy it to the server by hand if you want it there.
It is fully independent: it follows the containers by name
(`paybridge-standalone-back`, `-front`, `-postgres`), so it needs neither
compose, `.env.preprod` nor `VERSION`.

```bash
./logs.sh            # asks what to follow
./logs.sh backend    # or: frontend | db  (one container at a time)
```

## Manual rollback on the host :: !! NO JENKINS !!

`deploy.sh` reads the version from the `VERSION` file ; 

Rolling back manually is basically one-liner: 
Just specify the version we want to rollback to like shown below

```bash
./deploy.sh 1.0.15
```
The previous images are still in the registry, so it's just a redeploy, not a rebuild.


## Reminder of manual actions if Jenkins is down
Manual (build/push) from each repo root:

```bash
docker build -t sahelys/paybridge-standalone:<V> -t sahelys/paybridge-standalone:latest .   # backend repo
docker build -t sahelys/paybridge-front:<V>      -t sahelys/paybridge-front:latest .        # frontend repo
docker push ...   # all four tags -- never push only one
```
