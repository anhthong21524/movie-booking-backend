# Movie Booking API — Claude Code Guide

## Project overview

Quarkus 3.34.2 modular monolith for a movie booking system. Java 21, Gradle 9.3, PostgreSQL 16, Liquibase.
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
  auth/          # Register, login, oauth, refresh, logout
  user/          # Current user profile + avatar
  movie/         # Movie catalogue + admin CRUD
  showtime/      # Showtime listing + seat availability + admin CRUD
  booking/       # Booking creation, cancellation, history
  common/
    exception/   # AppException, GlobalExceptionMapper
    response/    # ProblemDetail, PageResponse<T>, FieldError
    util/        # SortUtil

src/main/resources/
  application.properties
  META-INF/resources/
    privateKey.pem   # Dev RSA key (committed — dev only)
    publicKey.pem    # Dev RSA public key
  db/changelog/
    db.changelog-master.yaml
    V1__init_schema.yaml              # users, movies, showtimes
    V2__add_booking.yaml              # bookings, booking_seats
    V3__constraints.yaml              # unique indexes, FK indexes
    V4__update_schema.yaml            # new columns, cinemas, rooms, seats tables
    V5__seed_data.yaml                # test data (cinemas, rooms, users, movies, showtimes, seats)
    V6__add_user_avatar.yaml          # users.avatar column
    V7__fix_seats_row_column_type.yaml # seats.row CHAR(1) → VARCHAR(1)

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
| `User`        | `users`        | `id`, `email`, `password`, `role`, `name`, `avatar`, `created_at` |
| `Movie`       | `movies`       | `id`, `title`, `description`, `duration_minutes`, `poster_url`, `status`, `genre`, `rating`, `release_date`, `base_price`, `created_at`, `updated_at` |
| `Showtime`    | `showtimes`    | `id`, `movie_id`, `room`, `start_time`, `end_time`, `total_seats`, `available_seats`, `price`, `created_at`, `updated_at` |
| `Seat`        | `seats`        | `id`, `showtime_id`, `row`, `number`, `label`, `status` |
| `Booking`     | `bookings`     | `id`, `user_id`, `showtime_id`, `status`, `idempotency_key`, `unit_price`, `total_amount`, `confirmed_at`, `created_at`, `updated_at` |
| `BookingSeat` | `booking_seats`| `id`, `booking_id`, `showtime_id`, `seat_number` |

**Critical constraints:**
- `users.email` — unique
- `seats(showtime_id, row, number)` — unique
- `booking_seats(showtime_id, seat_number)` — unique (double-booking guard at DB level)
- `bookings.idempotency_key` — unique (nullable)

**Seed data** (V5 migration — dev/staging):
- 3 cinemas, 9 rooms (capacity 60 each)
- Test users: `user@moviehub.test / user1234`, `admin@moviehub.test / admin1234`
- 3 movies, 6 showtimes, 360 seats with pre-booked patterns

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
| 403 | `FORBIDDEN` | Authenticated but wrong role or not owner |
| 404 | `USER_NOT_FOUND` | No user with given ID |
| 404 | `MOVIE_NOT_FOUND` | No movie with given ID |
| 404 | `SHOWTIME_NOT_FOUND` | No showtime with given ID |
| 404 | `BOOKING_NOT_FOUND` | No booking with given ID |
| 409 | `EMAIL_ALREADY_EXISTS` | Duplicate email on register |
| 409 | `MOVIE_HAS_SHOWTIMES` | Delete movie that has showtimes |
| 409 | `SHOWTIME_OVERLAP` | New showtime collides in same room (field error on `room`) |
| 409 | `SEAT_ALREADY_RESERVED` | One or more seats already booked (field error on `seatIds`) |
| 409 | `BOOKING_ALREADY_CANCELLED` | Cancelling an already-cancelled booking |
| 500 | `INTERNAL_ERROR` | Unhandled server exception |

---

## API endpoints

### Auth — `POST /api/v1/auth/**` (public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Register; returns `AuthResponse` (201) |
| POST | `/auth/login` | Login with email+password; returns `AuthResponse` |
| POST | `/auth/oauth` | OAuth login, auto-creates user if new; returns `AuthResponse` |
| POST | `/auth/refresh` | Exchange refresh token for new access token |
| POST | `/auth/logout` | Client-side logout (authenticated; 204) |

### User — authenticated

| Method | Path | Description |
|--------|------|-------------|
| GET | `/users/me` | Get current user profile |
| PUT | `/users/me/avatar` | Update avatar (`data:image/...` URL) |
| DELETE | `/users/me/avatar` | Remove avatar |
| GET | `/users/me/bookings?page&size&sort&status` | Paginated booking history |

### Movies — `GET /api/v1/movies/**` (public), `POST/PUT/DELETE /api/v1/admin/movies/**` (ADMIN)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/movies?page&size&sort&status&keyword` | Paginated list with optional filters |
| GET | `/movies/{id}` | Movie by ID |
| POST | `/admin/movies` | Create movie |
| PUT | `/admin/movies/{id}` | Update movie (full replace) |
| DELETE | `/admin/movies/{id}` | Delete movie (fails if has showtimes) |

### Showtimes — public GET, ADMIN write

| Method | Path | Description |
|--------|------|-------------|
| GET | `/showtimes?movieId&fromTime&toTime` | List showtimes for movie/date range |
| GET | `/showtimes/{id}` | Showtime details |
| GET | `/showtimes/{id}/seats` | Seat grid with availability |
| GET | `/admin/showtimes?page&size&sort&movieId&fromTime&toTime` | Paginated admin list |
| POST | `/admin/showtimes` | Create showtime |
| PUT | `/admin/showtimes/{id}` | Update showtime |
| DELETE | `/admin/showtimes/{id}` | Delete showtime |

### Bookings — authenticated

| Method | Path | Description |
|--------|------|-------------|
| GET | `/bookings` | All user bookings (fast, unpaginated) |
| POST | `/bookings` | Create booking (with optional `Idempotency-Key` header) |
| GET | `/bookings/{id}` | Booking details (own only) |
| PATCH | `/bookings/{id}` | Cancel booking (own only, `{"status": "CANCELLED"}`) |

---

## Security

| Path pattern | Access |
|---|---|
| `POST /api/v1/auth/**` | Public |
| `GET /api/v1/movies/**` | Public |
| `GET /api/v1/showtimes/**` | Public |
| `GET /api/v1/users/me` | `USER` or `ADMIN` |
| `PUT/DELETE /api/v1/users/me/avatar` | `USER` or `ADMIN` |
| `GET /api/v1/users/me/bookings` | `USER` or `ADMIN` |
| `/api/v1/bookings/**` | `USER` or `ADMIN` |
| `/api/v1/admin/**` | `ADMIN` only |

**JWT (RS256):**
- Access token: 15 min TTL, claims `sub` (userId), `email`, `groups` (role)
- Refresh token: 7 day TTL, claim `type: "refresh"`
- Dev keys committed under `META-INF/resources/`. In production, supply via `mp.jwt.verify.publickey` and `smallrye.jwt.sign.key` env vars.
- Logout is client-side only — no server-side revocation (see `docs/decisions.md` #4).

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

    @Mock BookingRepository bookingRepository;
    @Mock SeatRepository seatRepository;

    @InjectMocks BookingService bookingService;

    @Test
    void createBooking_throwsWhenSeatAlreadyReserved() { ... }
}
```

---

## Common pitfalls

- **`availableSeats` must be updated in the same transaction** as inserting `booking_seats` and marking seats BOOKED. Never split across calls.
- **`PENDING → CONFIRMED` happens in the same transaction** as seat reservation. Do not split.
- **Cancellation reversal**: also mark seats AVAILABLE and increment `availableSeats` atomically.
- **Showtime overlap check**: `newStart < existingEnd AND newEnd > existingStart` for same room. Exact predicate in JPQL.
- **Idempotency key** is nullable. When present, check for existing booking before inserting and return original response if found.
- **Room pattern**: `^[A-Z0-9][A-Z0-9_-]*$`. Validate at resource layer.
- **Avatar validation**: must be a `data:image/...` URL. Validate in service before persisting.
- **Logout is stateless**: JWT remains valid until expiry even after logout call.
- **Never call `entityManager.flush()` manually** — let the transaction boundary handle it.
- **Seat label** is stored on `BookingSeat.seatNumber` as the label string (e.g. `"A5"`), not the seat ID.

---

## Environment variables

```
DB_URL=jdbc:postgresql://localhost:5432/movie_booking
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_ISSUER=movie-booking-api
JWT_ACCESS_EXPIRY=900
JWT_REFRESH_EXPIRY=604800
```

See `.env.example`. In production on Render, all vars are injected via `render.yaml`.

---

## Database

Liquibase runs automatically on startup (`migrate-at-start=true`).
Add new migrations as **new** changelog files — **never edit existing ones**.
Naming: `V{n}__{description}.yaml`.

---

## What is NOT in scope (v1)

Payment, email/SMS notifications, Redis cache, Kafka, microservices, seat map UI, multi-tenant, pricing engine, refresh token blacklist.
