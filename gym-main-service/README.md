# gym-management-service

The main REST application — trainees, trainers, trainings, and JWT authentication. Part of the [Gym Management microservices project](../README.md); see the root README for building, running the full stack, and Docker.

## Tech stack

- Java 21 / Spring Boot 3.2
- Spring Security 6 + JWT (BCrypt, brute-force protection)
- Hibernate / JPA — H2 (local) or PostgreSQL (dev/stg/prod/docker)
- Swagger / springdoc-openapi, Actuator + Prometheus

## Run this service alone

```bash
export JWT_SECRET=$(openssl rand -hex 32)
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl gym-main-service spring-boot:run
```

Uses the `local` profile by default (H2 in-memory, schema + seed loaded automatically). Full workload notifications require Eureka and the workload service running too — see the root README.

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console
- Actuator: http://localhost:8080/actuator

Other profiles: `-Dspring-boot.run.profiles=dev` (or `stg` / `prod` / `docker`).

## API overview

| Method | Endpoint | Auth required |
|--------|----------|---------------|
| POST | `/trainees` | No |
| POST | `/trainers` | No |
| POST | `/auth/login` | No |
| POST | `/auth/logout` | Yes |
| GET/PUT/DELETE | `/trainees/**` | Yes |
| GET/PUT | `/trainers/**` | Yes |
| POST | `/api/trainings` | Yes |
| GET | `/api/training-types` | No |

Protected endpoints require `Authorization: Bearer <token>`.

## Security

- Passwords stored with BCrypt
- Login returns a signed JWT valid for 1 hour
- Logout blacklists the token
- Brute-force protection: account locked for 5 minutes after 3 failed login attempts
- CORS origins configured per profile via `cors.allowed-origins`

## Profiles

| Profile | Database |
|---------|----------|
| `local` (default), `dev` | H2 in-memory |
| `stg`, `prod` | PostgreSQL (env-driven connection) |
| `docker` | PostgreSQL (Hibernate-managed schema; used by docker-compose) |

## Tests

```bash
mvn test
mvn test -Dtest=TraineeServiceImplTest
mvn test -Dtest=TraineeServiceImplTest#create_ShouldSetUsernamePasswordAndActive_AndSave
```
