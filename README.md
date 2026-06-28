# Gym Management Service

A Spring Boot REST API for managing gym trainees, trainers, and training sessions.

## Tech Stack

- Java 21 / Spring Boot 3.2
- Spring Security 6 + JWT
- Hibernate / JPA
- H2 (local), PostgreSQL (dev/stg/prod)
- Swagger / springdoc-openapi
- Spring Actuator + Prometheus

## Getting Started

### Prerequisites

- JDK 21
- Maven 3.8+
- A JWT secret

### Environment Variables

Create a `.env` file or export these before running:

```bash
JWT_SECRET=<hex string, min 32 bytes>
JWT_EXPIRATION_MS=3600000
```

### Run locally

```bash
mvn spring-boot:run
```

Uses the `local` profile by default - H2 in-memory database, schema and seed data loaded automatically.

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console
- Actuator: http://localhost:8080/actuator

### Other profiles

```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

## API Overview

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

Authentication uses `Authorization: Bearer <token>` on all protected endpoints.

## Security

- Passwords stored with BCrypt
- Login returns a signed JWT valid for 1 hour
- Logout blacklists the token
- Brute-force protection: an account locked for 5 minutes after 3 failed login attempts
- CORS origins configured per profile via `cors.allowed-origins` property

## Running Tests

```bash
mvn test

# Single class
mvn test -Dtest=TraineeServiceImplTest

# Single method
mvn test -Dtest=TraineeServiceImplTest#create_ShouldSetUsernamePasswordAndActive_AndSave
```
