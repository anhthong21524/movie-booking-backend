# Movie Booking API — Claude Code Guide

## Project overview

Quarkus 3.x modular monolith for a movie booking system. Java 21, Gradle, PostgreSQL, Liquibase.
See `movie-booking-api-spec.yaml` for the full API contract and `docs/decisions.md` for design rationale.

---

## Common commands

```bash
# Start in dev mode (hot reload)
./gradlew quarkusDev

# Build (produces build/quarkus-app/)
./gradlew build

# Run tests
./gradlew test

# Start local PostgreSQL only
docker compose up postgres -d

# Start full stack
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
  common/
    exception/   # AppException, GlobalExceptionMapper
    response/    # ProblemDetail, PageResponse<T>, FieldError

src/main/resources/
  application.properties
  META-INF/resources/
    privateKey.pem   # Dev RSA key (committed — dev only)
    publicKey.pem    # Dev RSA public key
  db/changelog/
    db.changelog-master.yaml
    V1__init_schema.yaml
    V2__add_booking.yaml
    V3__constraints.yaml

docs/
  decisions.md   # Design rationale for non-obvious choices
```

Each module follows the same layered structure:
`dto/` → `entity/` → `repository/` → `service/` → `resource/`

---

## Architecture rules

- **No records, no pattern matching, no switch expressions** — plain classes only.
- **Layers are strict**: Resource → Service → Repository. No cross-layer skipping.
- **DTOs cross layer boundaries**; entities never leave the service layer.
- **Transactions** belong in the Service layer (`@Transactional`).
- **Validation** on incoming DTOs via `@Valid` + Hibernate Validator annotations.
- **Error responses** always use `ProblemDetail` shape (`code`, `detail`, optional `errors[]`).
- **No `null` returns** from service methods — throw a typed domain exception instead.
- **Never expose entities directly** from Resource methods — always map to a DTO first.

---

## Data model

| Java Class    | Table          | Key columns |
|---------------|----------------|-------------|
| `User`        | `users`        | `id`, `email`, `password`, `role`, `created_at` |
| `Movie`       | `movies`       | `id`, `title`, `description`, `duration_minutes`, `poster_url`, `status`, `created_at`, `updated_at` |
| `Showtime`    | `showtimes`    | `id`, `movie_id`, `room`, `start_time`, `end_time`, `total_seats`, `available_seats`, `created_at`, `updated_at` |
| `Booking`     | `bookings`     | `id`, `user_id`, `showtime_id`, `status`, `idempotency_key`, `created_at`, `updated_at` |
| `BookingSeat` | `booking_seats`| `id`, `booking_id`, `showtime_id`, `seat_number` |

**Critical constraints:**
- `users.email` — unique
- `booking_seats(showtime_id, seat_number)` — unique (double-booking guard)
- `bookings.idempotency_key` — unique (nullable)

---

## Enum reference

```
UserRole:      USER | ADMIN
MovieStatus:   NOW_SHOWING | COMING_SOON | ARCHIVED
BookingStatus: PENDING | CONFIRMED | CANCELLED
SeatStatus:    AVAILABLE | HELD | BOOKED
```

---

## Error codes reference

| HTTP | Code | Trigger |
|------|------|---------|
| 400 | `VALIDATION_ERROR` | Bean validation failure |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT |
| 403 | `FORBIDDEN` | Authenticated but wrong role |
| 404 | `MOVIE_NOT_FOUND` | No movie with given ID |
| 404 | `SHOWTIME_NOT_FOUND` | No showtime with given ID |
| 404 | `BOOKING_NOT_FOUND` | No booking with given ID |
| 409 | `EMAIL_ALREADY_EXISTS` | Duplicate email on register |
| 409 | `SHOWTIME_OVERLAP` | New showtime collides with existing one in same room |
| 409 | `SEAT_ALREADY_RESERVED` | One or more seats already booked |
| 409 | `BOOKING_ALREADY_CANCELLED` | Cancelling an already-cancelled booking |

---

## Security

| Path pattern | Access |
|---|---|
| `POST /api/v1/auth/**` | Public |
| `GET /api/v1/movies/**` | Public |
| `GET /api/v1/showtimes/**` | Public |
| `GET /api/v1/users/me` | Authenticated (`USER` or `ADMIN`) |
| `POST /api/v1/bookings` | Authenticated |
| `PATCH /api/v1/bookings/{id}` | Authenticated (own booking only) |
| `GET /api/v1/users/me/bookings` | Authenticated |
| `/api/v1/admin/**` | `ADMIN` role only |

JWT RS256. Dev keys committed under `src/main/resources/META-INF/resources/`.
In production, supply via `mp.jwt.verify.publickey` and `smallrye.jwt.sign.key` env vars.

---

## Test strategy

**Unit tests only — no DB, no Quarkus context.**

- Use plain JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- Mock repositories in service tests; mock services in resource tests
- Test classes live in `src/test/java/com/moviebooking/`
- One test class per production class (e.g. `MovieServiceTest`, `BookingServiceTest`)
- Focus on business logic: validation, conflict detection, state transitions

```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    BookingRepository bookingRepository;

    @InjectMocks
    BookingService bookingService;

    @Test
    void createBooking_throwsWhenSeatAlreadyReserved() { ... }
}
```

---

## Common pitfalls

- **`availableSeats` must be updated in the same transaction** as inserting `booking_seats`. Never update it in a separate call.
- **Logout does not revoke the JWT server-side** (stateless by design). See `docs/decisions.md` #4.
- **`PENDING → CONFIRMED` happens in the same transaction** as seat reservation in v1. Do not split them.
- **Showtime overlap check**: overlapping means `newStart < existingEnd AND newEnd > existingStart` for the same room. Use this exact predicate in the JPQL query.
- **Idempotency key** is nullable. When present, check for an existing booking with that key before inserting. Return the original response if found.
- **Seat number pattern** is `^[A-Z][0-9]{1,2}$` (e.g. `A1`, `B12`). Validate at the resource layer.
- **Never call `entityManager.flush()` manually** — let the transaction boundary handle it.

---

## Environment variables

See `.env.example`. In production on Render, all vars are injected via `render.yaml`.

---

## Database

Liquibase runs automatically on startup (`migrate-at-start=true`).
Add new migrations as new changelog files — **never edit existing ones**.
Naming: `V{n}__{description}.yaml`.

---

## What is NOT in scope (v1)

Payment, email/SMS notifications, Redis cache, Kafka, microservices, seat map UI, multi-tenant, pricing engine, refresh token blacklist.
