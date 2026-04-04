# Movie Booking API — Claude Code Guide

## Project overview

Quarkus 3.x modular monolith for a movie booking system. Java 21, Gradle, PostgreSQL, Liquibase.

## Common commands

```bash
# Start in dev mode (hot reload)
./gradlew quarkusDev

# Build (produces build/quarkus-app/)
./gradlew build

# Run tests
./gradlew test

# Start local infrastructure (PostgreSQL only)
docker compose up postgres -d

# Start full stack
docker compose up --build
```

## Project structure

```
src/main/java/com/moviebooking/
  auth/          # Register, login, refresh, logout
  user/          # Current user profile
  movie/         # Movie catalogue + admin CRUD
  showtime/      # Showtime listing + admin CRUD
  booking/       # Booking creation, cancellation, history
  common/
    exception/   # Global exception mapper, domain exceptions
    response/    # Shared response wrappers (ProblemDetail, PageResponse)

src/main/resources/
  application.properties       # All config; env-var overrides preferred
  db/changelog/                # Liquibase YAML changelogs
    db.changelog-master.yaml
    V1__init_schema.yaml
    V2__add_booking.yaml
    V3__constraints.yaml
```

Each module follows the same layered structure:
`dto/` → `entity/` → `repository/` → `service/` → `resource/`

## Architecture rules

- **No records, no pattern matching, no switch expressions** — plain classes only.
- **Layers are strict**: Resource calls Service; Service calls Repository. No cross-layer skipping.
- **DTOs cross layer boundaries**; entities never leave the service layer.
- **Transactions** belong in the Service layer (`@Transactional`).
- **Validation** on incoming DTOs via `@Valid` + Hibernate Validator annotations.
- **Error responses** always use `ProblemDetail` shape (`code`, `detail`, optional `errors[]`).
- **No `null` returns** from service methods — throw a typed exception instead.

## API contract

See `movie-booking-api-spec.yaml` (OpenAPI 3.1) at the project root. All endpoints, request/response shapes, and error codes are defined there. Implementation must match the spec exactly.

## Security

- Public endpoints: `GET /movies/**`, `GET /showtimes/**`, `POST /auth/**`
- Authenticated: all `/bookings/**`, `GET /users/me`
- Admin only: all `/admin/**`
- JWT RS256. Keys live in `src/main/resources/META-INF/resources/` (dev only; use env vars in prod).

## Environment variables

See `.env.example`. Never commit `.env` or real credentials.

## Database

Liquibase runs automatically on startup (`migrate-at-start=true`).
Add new migrations as new changelog files; never edit existing ones.
Changelog file naming: `V{n}__{description}.yaml`.

## What is NOT in scope (v1)

Payment, email/SMS notifications, Redis cache, Kafka, microservices, seat map UI, multi-tenant, pricing engine.
