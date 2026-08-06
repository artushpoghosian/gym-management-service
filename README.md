# Gym Management — Microservices

A multi-module Spring Boot 3.2 / Java 21 system for managing gym trainees, trainers, and trainings, split into three independent services with Eureka discovery, JWT service-to-service auth, and asynchronous messaging over ActiveMQ.

## Services

| Module | Port | Role |
|--------|------|------|
| [`eureka-server`](eureka-server) | 8761 | Netflix Eureka service registry (discovery) |
| [`gym-management-service`](gym-main-service) | 8080 | Main REST app: trainees, trainers, trainings, JWT auth (H2 / PostgreSQL) |
| [`trainer-workload-service`](trainer-workload-service) | 8082 | Tracks each trainer's monthly training minutes (MongoDB) |

**Flow:** when a training is created or deleted in the main service (or a trainee is deleted), it publishes the workload change **asynchronously** to an ActiveMQ queue; the workload service consumes it with a JMS listener. Each message carries a service JWT and the `transactionId` (stamped by a single message post-processor). Because the queue is durable, if the workload service is down the messages **wait in the queue** and are processed on recovery — nothing is lost. Messages that fail validation (missing required data) are routed to a dead-letter queue (`trainer.workload.dlq`) instead of being dropped. The workload service stores each trainer's summary in **MongoDB** — one document per trainer with a nested `Years → Months → summary duration` structure — and applies each event as a single atomic update. Reading a trainer's summary stays synchronous REST.

See [gym-main-service/README.md](gym-main-service/README.md) for the main service's endpoints and internals.

## Tech stack

- Java 21 / Spring Boot 3.2, Spring Cloud 2023.0.3 (Eureka)
- ActiveMQ Classic (JMS) for asynchronous service-to-service messaging
- Spring Security 6 + JWT (JJWT 0.12), BCrypt
- Hibernate / JPA — H2 (local) or PostgreSQL (Docker / non-local profiles) for the main service
- Spring Data MongoDB — trainer workload store (Testcontainers for integration tests)
- Jib for container images, Docker Compose for orchestration
- Swagger / springdoc-openapi, Actuator + Prometheus

## Prerequisites

- JDK 21 (Maven often defaults to another JDK — prefix commands with `JAVA_HOME=$(/usr/libexec/java_home -v 21)`)
- Maven 3.8+
- Docker (only for the Docker workflow)

## Build & test

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean package   # build all modules
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test            # fast: unit + slice tests
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean verify    # + Cucumber BDD suites (needs Docker)
```

Tests come in three layers: JUnit/Mockito unit + slice tests (`mvn test`), Cucumber **component**
tests per service, and a Cucumber **integration** suite that runs both services together — the
BDD suites run at `verify` on Testcontainers (Postgres, Mongo, ActiveMQ), so Docker must be
running. See the "Testing (three layers)" section in [CLAUDE.md](CLAUDE.md) for details.

## Run locally (order matters)

Export the **same** `JWT_SECRET` in every terminal (a hex string of at least 32 bytes; env vars do not carry between terminals). The workload service needs a running **MongoDB** (defaults to `mongodb://localhost:27017/trainer_workload`; override with `MONGODB_URI`) — start one with `docker run -d -p 27017:27017 mongo:7`. Start Eureka first, then the workload service, then the main service.

```bash
export JWT_SECRET=$(openssl rand -hex 32)   # same value in all three terminals
docker run -d -p 27017:27017 mongo:7        # MongoDB for the workload service

JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl eureka-server spring-boot:run
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl trainer-workload-service spring-boot:run
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl gym-main-service spring-boot:run
```

- Eureka dashboard: http://localhost:8761
- Main service Swagger: http://localhost:8080/swagger-ui.html

## Run with Docker

Images are built with Jib (no Dockerfiles); Compose runs the full stack including PostgreSQL (the main service uses the `docker` profile → Postgres instead of H2), **MongoDB** (the workload store), and an ActiveMQ broker (console at http://localhost:8161, `admin`/`admin`).

```bash
cp .env.example .env            # then set JWT_SECRET (openssl rand -hex 32)
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean package jib:dockerBuild
docker compose up -d
```

Compose injects one `JWT_SECRET` from `.env` into both services, so they can't drift out of sync. Images build for `arm64` by default (Apple Silicon); for x86 add `-Djib.platform.architecture=amd64`.

Stop it:

```bash
docker compose down        # stop and remove containers (Postgres + Mongo volumes kept)
docker compose down -v     # also wipe the Postgres and Mongo data volumes
```

## End-to-end test

With all services running (locally or via Docker):

```bash
./smoke-test.sh
```

Registers a trainer + trainee, logs in, creates trainings, and reads the trainer's monthly summary back from the workload service. It only makes HTTP calls, so it does not need `JWT_SECRET`.
