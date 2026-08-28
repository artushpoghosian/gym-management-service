# Running the gym stack in Docker

Notes from getting this working locally. Tested on Docker Engine 29.5.3, Apple Silicon.

You need Docker running and `openssl` for the secret. You don't need Java or Maven
installed, since each Dockerfile builds its own jar inside the image.

## Setup

```bash
cp .env.example .env
openssl rand -hex 32     # put this in .env as JWT_SECRET
```

Compose picks up `.env` on its own. The plain `docker run` commands further down don't,
so export the same value in your shell:

```bash
export JWT_SECRET=<same 64-hex value>
```

Both services have to get the *same* secret, because the workload service validates
tokens the main service signs. It also has to be exactly 64 hex chars: the code runs it
through `HexFormat.parseHex`, and anything else blows up at startup.

## Subtask 1: Dockerfiles, images, running with integrations off

There are three Dockerfiles, one per service:

| Dockerfile | Image | Port |
|---|---|---|
| `gym-main-service/Dockerfile` | `gym/gym-management-service` | 8080 |
| `trainer-workload-service/Dockerfile` | `gym/trainer-workload-service` | 8082 |
| `eureka-server/Dockerfile` | `gym/eureka-server` | 8761 |

All three are two-stage. The first stage runs Maven (`maven:3.9-eclipse-temurin-21`) and
the second stage just runs the jar on `eclipse-temurin:21-jre` as UID 1000. `curl` gets
installed in the runtime stage, since the compose healthchecks use it and it's handy when
you shell in later.

Two things about the build stage that aren't obvious:

* It copies the parent pom *and all four module poms* before copying any source. Maven
  reads the whole reactor even when you ask for one module with `-pl`, so it will fail if
  a sibling pom is missing. Copying poms first also means the dependency layer gets reused
  when only source changes.
* The build runs `package spring-boot:repackage`, not just `package`. This project's
  parent isn't `spring-boot-starter-parent`, so the Spring Boot plugin has no `repackage`
  execution bound and you get a plain jar that dies with `no main manifest attribute`.
  Jib never cared because it builds from `target/classes`. I left `repackage` out of the
  poms on purpose: the `integration-tests` module depends on the module jars, and turning
  those into fat jars would break it.

Tests are skipped in the build. The Cucumber suites need a Docker daemon and there isn't
one inside a build container.

Build context is the repo root for all three, hence the `-f <module>/Dockerfile .` form:

```bash
docker build -f gym-main-service/Dockerfile -t gym/gym-management-service:latest .
docker build -f trainer-workload-service/Dockerfile -t gym/trainer-workload-service:latest .
docker build -f eureka-server/Dockerfile -t gym/eureka-server:latest .
```

Cold that's a few minutes, warm it's seconds. All three share a
`--mount=type=cache,target=/root/.m2`, so the Maven downloads happen once. Careful with
"clean rebuild" claims: `docker rmi` only removes the tag, the layer cache survives it and
the rebuild finishes suspiciously fast. Use `--no-cache` if you actually want to recompile.

### What "integrations disabled" means here

Both services have a `standalone` profile that switches the outside world off:

| | main | workload |
|---|---|---|
| DB | H2 in memory | Mongo untouched at boot (`auto-index-creation: false`) |
| queue | listener off | listener `auto-startup: false` |
| discovery | `eureka.client.enabled=false` | same |
| health probes | jms, discovery off | jms, mongo, discovery off |

That last row took a second pass. Without it the app comes up fine but
`/actuator/health` returns DOWN, because the JMS indicator keeps probing a broker that
isn't supposed to exist, and then every container healthcheck fails.

### Running them

```bash
docker run -d --name gym-main-standalone -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=standalone -e JWT_SECRET=$JWT_SECRET \
  gym/gym-management-service:latest

docker run -d --name gym-workload-standalone -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=standalone -e JWT_SECRET=$JWT_SECRET \
  gym/trainer-workload-service:latest

docker run -d --name eureka-standalone -p 8761:8761 gym/eureka-server:latest
```

No `--network`, no Postgres, no broker, no registry. Each container is on its own.

Checks:

```bash
docker logs gym-main-standalone | grep "Started GymApp"
curl -s localhost:8080/actuator/health
curl -s -X POST localhost:8080/trainees \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ken","lastName":"Gym"}'
```

Health comes back `UP` with `"database":"H2"`, and the POST returns something like
`{"username":"ken.gym","password":"3f16bc602e"}`. I use the POST rather than just the
health endpoint because it proves `schema.sql` and `data.sql` actually loaded, so the
service is doing real work and not merely answering pings.

The workload service is a bit different:

```bash
docker logs gym-workload-standalone | grep "Started TrainerWorkloadApplication"
curl -s localhost:8082/api/trainer-workload/anyone
```

You get a 401 and `{"error":"Unauthorized","message":"Authentication required"}`. That's
the good outcome. Its `SecurityConfig` is `anyRequest().authenticated()` with no public
paths at all, so even `/actuator/health` is 401. The JSON comes from the service's own
entry point, which means Tomcat, Spring MVC and the filter chain are all up.

Eureka is the easy one, `curl -s localhost:8761/actuator/health` gives `{"status":"UP"}`
and the dashboard loads at http://localhost:8761. It needs no `JWT_SECRET`, it doesn't
touch tokens.

Worth grepping the logs of both services for `Connection refused`,
`MongoSocketOpenException` and `Could not refresh JMS Connection`. There should be nothing.

Then clean up:

```bash
docker rm -f gym-main-standalone gym-workload-standalone eureka-standalone
```

## Subtask 2: network config, integrations back on

### Doing it by hand

```bash
docker network create --driver bridge gym-net
docker network inspect gym-net --format '{{.Name}} {{.Driver}} {{(index .IPAM.Config 0).Subnet}}'
```

Mine came out as `gym-net bridge 172.26.0.0/16`.

It has to be a user-defined bridge, not the default one. Only user-defined networks give
you DNS resolution by container name, and every connection string below leans on that:
`mongodb://gym-mongo:27017/...`, `tcp://gym-activemq:61616`,
`http://eureka-server:8761/eureka/`.

If you've run this before, the names will still be taken:

```bash
docker rm -f gym-management-service trainer-workload-service eureka-server \
             gym-postgres gym-activemq gym-mongo
```

Infrastructure first. Postgres has no `-p` on purpose, nothing outside the network talks
to it:

```bash
docker run -d --name gym-postgres --network gym-net \
  -e POSTGRES_DB=gymdb -e POSTGRES_USER=gym -e POSTGRES_PASSWORD=gym \
  postgres:16-alpine

docker run -d --name gym-mongo --network gym-net -p 27017:27017 mongo:7

docker run -d --name gym-activemq --network gym-net \
  -p 61616:61616 -p 8161:8161 apache/activemq-classic:latest
```

Give them ~15 seconds, ActiveMQ is the slow one, then:

```bash
docker exec gym-postgres pg_isready -U gym -d gymdb
docker exec gym-mongo mongosh --quiet --eval "db.adminCommand('ping')"
```

Now Eureka, then the two apps:

```bash
docker run -d --name eureka-server --network gym-net -p 8761:8761 \
  -e EUREKA_HOSTNAME=eureka-server gym/eureka-server:latest

docker run -d --name trainer-workload-service --network gym-net -p 8082:8082 \
  -e JWT_SECRET=$JWT_SECRET \
  -e MONGODB_URI=mongodb://gym-mongo:27017/trainer_workload \
  -e ACTIVEMQ_BROKER_URL=tcp://gym-activemq:61616 \
  -e ACTIVEMQ_USER=admin -e ACTIVEMQ_PASSWORD=admin \
  -e EUREKA_URL=http://eureka-server:8761/eureka/ -e EUREKA_PREFER_IP=true \
  gym/trainer-workload-service:latest

docker run -d --name gym-management-service --network gym-net -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker -e JWT_SECRET=$JWT_SECRET \
  -e DB_HOST=gym-postgres -e DB_PORT=5432 -e DB_NAME=gymdb \
  -e DB_USERNAME=gym -e DB_PASSWORD=gym \
  -e ACTIVEMQ_BROKER_URL=tcp://gym-activemq:61616 \
  -e ACTIVEMQ_USER=admin -e ACTIVEMQ_PASSWORD=admin \
  -e EUREKA_URL=http://eureka-server:8761/eureka/ -e EUREKA_PREFER_IP=true \
  gym/gym-management-service:latest
```

Note what's different from subtask 1. The workload service gets no
`SPRING_PROFILES_ACTIVE` at all, so `standalone` isn't applied and Mongo, JMS and Eureka
are all live again. The main service gets `docker`, which points it at Postgres instead
of H2.

The check I'd actually show someone is one container reaching another by name:

```bash
docker exec gym-management-service \
  curl -s -o /dev/null -w '%{http_code}\n' http://eureka-server:8761/actuator/health
```

200 means the network config is doing its job. A couple more:

```bash
curl -s -H 'Accept: application/json' localhost:8761/eureka/apps \
  | grep -o 'GYM-MANAGEMENT-SERVICE\|TRAINER-WORKLOAD-SERVICE' | sort -u

docker exec gym-management-service curl -s localhost:8080/actuator/health
```

Both services show up in the registry, and health now says `"database":"PostgreSQL"` with
`"jms":{"status":"UP"}`, which is the contrast with subtask 1.

Then the real test:

```bash
./smoke-test.sh
```

It registers a trainer and a trainee, logs in, creates three trainings and reads the
monthly summary back off the workload service. You should see July = 90 and August = 45.
That only adds up if Postgres, ActiveMQ and Mongo are all reachable over `gym-net`.

### The same thing with compose

```bash
docker compose up -d --build
docker compose ps
./smoke-test.sh
```

`--build` uses the same three Dockerfiles (each service has a `build:` block). Compose
makes its own network, `gym-microservices_gym-net`, and orders startup with
`depends_on: condition: service_healthy`, so there's no waiting around by hand. All six
containers should end up `(healthy)` and the smoke test gives the same 90 / 45.

One naming trap in the compose file: the module directory is `gym-main-service` but the
service and image are called `gym-management-service`.

## Subtask 3: shelling in and reading logs

```bash
docker exec -it gym-management-service bash
```

Inside:

```bash
id
ls -l /app
ps -ef | grep [j]ava
curl -s localhost:8080/actuator/health
ls /app/*.log
```

You're `uid=1000(ubuntu)`, there's a single `app.jar` and one java process, health is UP,
and there are no log files. That last one is the interesting bit: both services only
configure `logging.pattern.console`, so everything goes to stdout, Docker catches it with
the json-file driver and `docker logs` reads it back. You can see where it lands:

```bash
docker inspect --format '{{.HostConfig.LogConfig.Type}} -> {{.LogPath}}' gym-management-service
```

The workload container works the same way, except its self health check answers 401.

Reading the logs:

```bash
docker logs --tail 40 gym-management-service
docker logs --tail 40 trainer-workload-service
docker compose logs -f --tail 20 gym-management-service trainer-workload-service
```

The thing worth demoing is following one request across both services. Generate traffic
first, then pull a transaction id out of the main service's log and look for it in the
other one:

```bash
./smoke-test.sh >/dev/null

TX=$(docker logs gym-management-service 2>&1 | grep 'Published ADD workload' \
     | tail -1 | sed -n 's/.*\[\([0-9a-f-]\{20,\}\)\].*/\1/p')
echo "$TX"
docker logs gym-management-service 2>&1 | grep "$TX"
docker logs trainer-workload-service 2>&1 | grep "$TX"
```

Run the smoke test *after* the last restart of either container, otherwise you'll pick an
id that no longer exists in `docker logs` (it only keeps output from the current start).
I lost a few minutes to that.

What comes out, same id in two different containers:

```
main      [TX-START] POST /api/trainings transactionId=fd09e469-...
main      [OP] createTraining called: name=Cardio C
main      Published ADD workload for trainer=nora.fit5
main      [TX-END] status=200
workload  [MQ] received ADD workload for trainer=nora.fit5
workload  ADD 45 min for trainer=nora.fit5 2026/8
```

`WorkloadMessagePostProcessor` puts the id on the JMS message as a string property and
`WorkloadMessageListener` reads it back into the MDC, so `%X{transactionId}` prints it on
both sides.

## Tearing down

```bash
docker compose down      # containers gone, Postgres and Mongo volumes kept
docker compose down -v   # wipe the volumes too
```

For the hand-run stack:

```bash
docker rm -f gym-management-service trainer-workload-service eureka-server \
             gym-postgres gym-activemq gym-mongo
docker network rm gym-net
```

## Things that went wrong

`no main manifest attribute` on startup means the jar wasn't repackaged. Don't drop
`spring-boot:repackage` from the Dockerfiles.

`Conflict. The container name ... is already in use` just needs a `docker rm -f <name>`,
usually leftovers from an earlier run.

A 401 from the workload service isn't a bug, every endpoint there is authenticated.

If the main service's health says DOWN with a jms error, you started it without the
`standalone` profile and without a broker. Either add
`-e SPRING_PROFILES_ACTIVE=standalone` or start ActiveMQ.

`Port is already allocated`: something already has 8080/8082/8761/27017, find it with
`lsof -i :8080`.

Jib still works if you'd rather not use the Dockerfiles, and produces the same image
names: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean package jib:dockerBuild`.
It's pinned to arm64 in the root pom (add `-Djib.platform.architecture=amd64` for x86),
whereas the Dockerfiles just build for whatever the host is.
