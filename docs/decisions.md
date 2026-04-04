# Design Decisions

Short rationale for non-obvious choices. Reference these before changing behaviour that seems wrong — it may be intentional.

---

## 1. PATCH instead of DELETE for booking cancellation

`PATCH /bookings/{id}` with `{ "status": "CANCELLED" }` rather than `DELETE /bookings/{id}`.

**Why:** Cancellation is a state transition, not a deletion. The booking record must be retained for history (`GET /users/me/bookings` returns cancelled bookings). A DELETE would lose that record.

---

## 2. `availableSeats` is a stored column, not computed

`showtimes.available_seats` is a persisted integer decremented on booking and incremented on cancellation, rather than computed as `total_seats - COUNT(confirmed booking_seats)`.

**Why:** The seat-availability check in `GET /showtimes` and the seat listing in `GET /showtimes/{id}/seats` are read-heavy. Storing the count avoids a full aggregate query on every request. The value is updated atomically inside the booking/cancellation transaction, so it stays consistent.

**Implication:** The booking service must update `available_seats` within the same transaction as inserting `booking_seats`.

---

## 3. Idempotency key is optional

`Idempotency-Key` header is optional on POST/PUT endpoints, not required.

**Why:** Not all clients support it. When absent, the request is processed normally with no deduplication. When present, the server returns the original response on retry without re-executing side effects. This matches Stripe-style idempotency semantics.

---

## 4. Stateless JWT — no refresh token blacklist in the database

Logout (`POST /auth/logout`) is implemented by discarding the token client-side. There is no server-side token revocation table.

**Why:** Keeping JWT stateless is an explicit v1 constraint from the project plan. A blacklist table would introduce a DB read on every authenticated request. Short access token lifetime (15 min) limits the exposure window on token leakage. A proper blocklist can be added in v2 if needed.

**Implication:** Logging out does not immediately invalidate the access token. The client must discard it.

---

## 5. `BookingSeat` is a separate table from `Booking`

Each booked seat is its own row in `booking_seats` rather than a JSON array column on `bookings`.

**Why:** The unique constraint `(showtime_id, seat_number)` on `booking_seats` is the primary guard against double-booking. This constraint cannot be enforced at the DB level on a JSON column. A separate table also allows per-seat queries and future per-seat pricing.

---

## 6. `BookingStatus` has a PENDING state

Bookings start as `PENDING`, not `CONFIRMED`.

**Why:** Reserved for a future payment step. In v1, the booking service immediately transitions `PENDING → CONFIRMED` within the same transaction. The `PENDING` state exists in the schema so v2 can introduce an async confirmation flow (e.g. after payment) without a migration.

---

## 7. Room identifier format enforced by pattern

`room` on showtimes is validated with `^[A-Z0-9][A-Z0-9_-]*$`.

**Why:** Prevents free-text values that would make overlap detection and display inconsistent. Examples: `ROOM_A`, `HALL_1`, `IMAX`.
