# Roadmap

## Original backlog

| # | Item | Status |
|---|------|--------|
| 1 | Update parking space by location and availability status (available/occupied) | Done - `PUT /api/parkingspace/update` (admin) |
| 2 | Spring Security and user authentication | Done - JWT login, `USER`/`ADMIN` roles, ownership checks |
| 3 | Update reservation by reservationId and duration, return `updatedAt` in the response | Done - `PUT /api/reservation/update`; responses include `updatedAt` |
| 4 | Unregistered users can see parking availability | Done - `GET /api/slots/**`, `GET /api/parkingspace/available`, `GET /api/parking/occupancy` are public |
| 5 | Find user by user name and email API | Open - service method exists, no endpoint |
| 6 | Password should not be exposed when retrieving user details | Done - passwords are BCrypt-hashed and never serialized |

## Done in the portfolio clean-up

- Secrets moved to environment variables (`.env.example`)
- Global JSON error handling with custom exceptions; no internal details in 500 responses
- Request DTOs with Bean Validation instead of `Map` bodies
- Billing fixes: per-ticket duplicate check, minimum one-hour charge, UUID invoice numbers, billing inside the exit transaction
- Optional admin seeding from `ADMIN_*` environment variables
- Unit and MockMvc integration tests on H2, GitHub Actions CI
- Dockerfile, Docker Compose (app + PostgreSQL 16), Swagger UI

## Done: correctness round (steps 1-4)

- **Flyway** migrations (V1 baseline of the old schema, V2-V4 changes); `ddl-auto=validate`; tests run the same migrations
- **Space status enum** `AVAILABLE` / `RESERVED` / `OCCUPIED` / `OUT_OF_SERVICE`; admins can only toggle
  `AVAILABLE` / `OUT_OF_SERVICE`, and not while a space is in use
- **Concurrency**: row lock on the space for every write path, fixed lock order, unique guard columns as a database
  safety net; tested with simultaneous requests
- **Reservation life cycle**: hold with expiry, fulfilled on entry and linked to the ticket, cancel instead of delete,
  move without extending the hold, scheduled expiry job plus expiry on the spot, one hold per user
- Injected `Clock`; feedback only for used reservations, once; removed the unused `allocateSlot`/`releaseSlot`
  methods that changed a space's status without a ticket; spaces with history are deactivated, not deleted

## Next

- Payment confirmation: users should not be able to mark their own bills as paid (admin confirmation or a mock
  payment provider with an idempotency key)
- Response DTOs for every endpoint (parking spaces and slots still return entities)
- Pagination for list endpoints
- Reservations for a future time slot (needs overlap checks against walk-ins)
- Configurable pricing and tax
- JWT subject based on user id (so renaming a user does not invalidate tokens), refresh tokens
- Use the vehicle type in slot recommendations
