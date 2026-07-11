# Digital Banking API

A Spring Boot REST API for a digital banking backend: account management, deposits/withdrawals, and transfers between accounts, with JWT authentication and role-based access control.

## Features

- **Account management** — create, view, list, and close accounts
- **Deposits & withdrawals** — with business-rule validation (positive amounts, active accounts, sufficient funds)
- **Transfers between accounts** — atomic, with pessimistic row locking and deadlock-safe lock ordering
- **JWT authentication** — register/login, stateless sessions, `USER`/`ADMIN` roles
- **Structured error responses** — consistent JSON shape (status, error code, message, field errors) across the whole API
- **Flyway-managed schema** on PostgreSQL
- **OpenAPI / Swagger UI** — browsable, interactive API docs
- **Test suite** — unit tests (Mockito) + integration tests against a real Postgres via Testcontainers, including concurrency tests that prove the locking guarantees under real parallel load

## Tech stack

Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate) · Spring Security · Flyway · PostgreSQL · JWT (`jjwt`) · springdoc-openapi · JUnit 5 · Mockito · Testcontainers · Maven · Docker

## Architecture notes

A few decisions worth calling out, since they're the parts most likely to come up in review:

- **Money is `BigDecimal`**, never `double` — no floating-point rounding errors on balances.
- **Optimistic locking (`@Version`)** on `Account` protects single-account operations (deposit/withdraw) from lost updates under concurrent requests, with `ObjectOptimisticLockingFailureException` mapped to a `409 Conflict`.
- **Pessimistic row locking** is used for transfers instead, since a transfer must hold both account rows for the duration of the operation rather than fail-and-retry. Lock acquisition order is always ascending by account number — regardless of transfer direction — so two concurrent transfers moving money in opposite directions between the same two accounts can't deadlock.
- **Routing uses the opaque `accountNumber`**, never the internal database `id` — avoids leaking a sequential, enumerable identifier (IDOR risk) through the public API.
- **A single `ApiException` base class** carries an HTTP status and machine-readable error code; every business exception just declares those two things, and one `@RestControllerAdvice` handler covers all of them — no per-exception boilerplate.
- **Business invariants live on the `Account` entity** (`credit`/`debit` methods), not scattered across services — deposit, withdraw, and transfer all funnel through the same validated methods.

## Getting started

### Option A: Docker (recommended — no Java/Maven required)

```bash
docker compose up --build
```

This builds the app image, starts PostgreSQL, waits for it to be healthy, then starts the API. Once it's up:

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health check: http://localhost:8080/health

### Option B: Local development (Maven + Docker for Postgres only)

Requires Java 21 and Maven.

```bash
docker compose up -d postgres
mvn spring-boot:run
```

## Using the API

1. Register a user: `POST /api/auth/register` with `{"username": "...", "password": "..."}`
2. Log in: `POST /api/auth/login` — returns a JWT
3. In Swagger UI, click **Authorize** and paste the token (no need to type "Bearer " — it's added automatically)
4. Call any endpoint under `/api/accounts` or `/api/transfers`

Note: `GET /api/accounts` (list all) and closing an account are **admin-only**. A dev-only admin account (`admin` / `admin12345`) is seeded automatically on startup — this is a local convenience, not something to carry into a real deployment.

## Running tests

```bash
mvn test
```

Integration tests spin up a real PostgreSQL container via Testcontainers automatically — Docker must be running, but no manual setup is needed.

## Project structure

```
account/    Account entity, repository, service, controller, DTOs
transfer/   Transfer service, controller, DTOs
auth/       User entity, JWT issuance/validation, auth endpoints
common/     Shared API error model, exception handling, OpenAPI config
```

## Configuration

Key environment variables (all have local-dev defaults in `application.properties`):

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | HMAC signing key for JWTs |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Postgres connection (overridden automatically inside Docker Compose) |
