# Movie Booking API

REST API for browsing movies, viewing showtimes, reserving seats, and managing bookings.

Built with **Quarkus 3.x**, **Java 21**, **Gradle**, **PostgreSQL**, and **Liquibase**.

---

## Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Quarkus 3.34.2 |
| Build | Gradle |
| Database | PostgreSQL 16 |
| Migrations | Liquibase |
| Auth | JWT (RS256) via SmallRye JWT |
| Validation | Hibernate Validator |
| Docs | OpenAPI 3.1 / Swagger UI |
| Deploy | Render (Docker / JVM) |

---

## Getting started

### Prerequisites

- Java 21
- Docker + Docker Compose

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env if needed (defaults work with docker-compose)
```

### 2. Start the database

```bash
docker compose up postgres -d
```

### 3. Run in dev mode

```bash
./gradlew quarkusDev
```

The app starts at `http://localhost:8080`. Liquibase migrations run automatically on startup.

---

## Useful endpoints

| Endpoint | Description |
|---|---|
| `GET /q/health` | Health check |
| `GET /swagger-ui` | Swagger UI |
| `GET /q/openapi` | Raw OpenAPI spec |

Full API contract: [`movie-booking-api-spec.yaml`](./movie-booking-api-spec.yaml)

---

## API overview

| Tag | Endpoints |
|---|---|
| Auth | `POST /api/v1/auth/register`, `/login`, `/refresh`, `/logout` |
| Users | `GET /api/v1/users/me` |
| Movies | `GET /api/v1/movies`, `GET /api/v1/movies/{id}` |
| Admin Movies | `POST/PUT/DELETE /api/v1/admin/movies` |
| Showtimes | `GET /api/v1/showtimes`, `GET /api/v1/showtimes/{id}/seats` |
| Admin Showtimes | `GET/POST/PUT/DELETE /api/v1/admin/showtimes` |
| Bookings | `POST /api/v1/bookings`, `PATCH /api/v1/bookings/{id}`, `GET /api/v1/users/me/bookings` |

---

## Build & run

```bash
# Build (produces build/quarkus-app/)
./gradlew build

# Run the built JAR
java -jar build/quarkus-app/quarkus-run.jar

# Run tests
./gradlew test

# Full stack with Docker
docker compose up --build
```

---

## Project structure

```
src/main/java/com/moviebooking/
  auth/          # Register, login, refresh, logout
  user/          # Current user profile
  movie/         # Movie catalogue + admin CRUD
  showtime/      # Showtime listing + admin CRUD
  booking/       # Booking creation, cancellation, history
  common/        # Exception handling, shared response types

src/main/resources/
  application.properties
  META-INF/resources/   # Dev JWT key pair
  db/changelog/         # Liquibase migrations
```

---

## Documentation

- [`CLAUDE.md`](./CLAUDE.md) — architecture rules, data model, error codes, test strategy
- [`docs/decisions.md`](./docs/decisions.md) — rationale for non-obvious design decisions
- [`CONTRIBUTING.md`](./CONTRIBUTING.md) — branch strategy and PR workflow
