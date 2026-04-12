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

---

## 8. `BookingSeat.seatNumber` stores the label string, not a FK to `seats`

`booking_seats.seat_number` is a `VARCHAR` like `"A5"`, not a foreign key to `seats.id`.

**Why:** The `seats` table is a live availability grid that can be repopulated or modified between migrations. Storing a FK would couple booking history to the current seat layout. The label string is stable, human-readable, and sufficient for displaying past bookings. The double-booking constraint `(showtime_id, seat_number)` on `booking_seats` uses this string, which is still unique per showtime.

**Implication:** When creating a booking, resolve seat IDs → labels before writing `BookingSeat` rows. Do not store raw IDs.

---

## 9. `Seat` table vs. `BookingSeat` table serve different purposes

Two tables track seats:
- `seats` — real-time availability grid (one row per physical seat per showtime, status: `AVAILABLE | HELD | BOOKED`)
- `booking_seats` — permanent record of which seats belong to a booking

**Why:** They answer different questions. `seats` answers "is this seat available right now?" and powers the seat picker UI. `booking_seats` answers "what seats did this booking include?" and must survive even if the `seats` grid is rebuilt. Keeping them separate avoids mutating booking history when availability data changes.

**Implication:** On booking, update both: mark `seats.status = BOOKED` and insert a `booking_seats` row. On cancellation, mark `seats.status = AVAILABLE` and leave `booking_seats` intact (the record remains).

---

## 10. OAuth login auto-creates users

`POST /auth/oauth` creates a new `USER`-role account if the email is not found, rather than returning 404.

**Why:** OAuth providers (Google, GitHub, etc.) guarantee the email is verified. Requiring a separate registration step adds friction with no security benefit. The provider is trusted; the email is the identity.

**Implication:** `oauthLogin` must normalize the email (lowercase, trim) before the existence check to prevent duplicate accounts from case variants.

---

## 11. `Showtime.price` overrides `Movie.basePrice`

Movies have a `base_price` and showtimes have a `price` column. The booking uses `showtime.price` for `unit_price`.

**Why:** Pricing varies by time slot, room type, and day (matinee vs. evening). `Movie.basePrice` is a catalogue default for display; `Showtime.price` is the actual charged price. This allows pricing flexibility without a separate pricing engine.

**Implication:** Always read price from the showtime, not the movie, when calculating `total_amount`.

---

## 12. `cinemas` and `rooms` tables exist but are not surfaced in the v1 API

V4 migration added `cinemas` and `rooms` tables. No endpoints or service logic use them yet.

**Why:** Added in anticipation of multi-cinema support (v2). Schema changes are cheaper to plan ahead; the tables are harmless and the seed data populates them so they're ready to use.

**Implication:** Do not add cinema/room filtering to existing endpoints without a deliberate v2 feature decision. The `room` field on `showtimes` is still a free-text identifier in v1.
