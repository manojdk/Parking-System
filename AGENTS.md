# AGENTS.md

A guide for AI coding agents (and new contributors) working on this repository. It covers what the project is, how
to build and test it, the rules the code relies on, and how changes are expected to be made. The README and the
`docs/` folder are the detailed reference; this file tells you what to read and what must not break.

## What this project is

A REST API for running a car park: parking spaces, vehicle entry and exit with tickets, automatic billing,
reservations, online payments through a mock provider, and feedback. It is a personal portfolio project. It is not
deployed and has no frontend: Swagger UI at `/swagger-ui.html` is the only client.

- Java 17, Spring Boot 3.3 (Web, Data JPA, Security, Validation), Lombok
- JWT auth (jjwt 0.11, HS256), BCrypt passwords, roles `USER` and `ADMIN`
- PostgreSQL 16 in normal use, H2 in PostgreSQL mode for tests, schema owned by Flyway
- springdoc-openapi 2.6, Maven wrapper, JUnit 5, Mockito, MockMvc, GitHub Actions

## Read these first

| File | Why |
|------|-----|
| `README.md` | Features, configuration (every environment variable), API overview, security model, test overview, known limitations |
| `docs/architecture.md` | Layers, the main flows, space and reservation state machines, **concurrency rules**, payments, pricing, slot types |
| `docs/database.md` | Every migration, every table and column, and the "active guard" column pattern |
| `docs/roadmap.md` | What is done and what is next |
| `docs/review-2026-10-12.html` | Historical: the review that found the original bugs, and the plan (steps 1-7) that fixed them. It shows the system *before* those fixes; open it in a browser |

## Build, test, run

```bash
./mvnw verify                          # full build and all tests, as CI runs it (Windows: mvnw.cmd verify)
./mvnw test -Dtest=PaymentIntegrationTest          # one test class
./mvnw test -Dtest='ConcurrencyIntegrationTest#*'  # one class, all methods
```

- Tests need **no database and no Docker**: the `test` profile (`src/test/resources/application-test.properties`)
  uses in-memory H2 in PostgreSQL mode, built by the same Flyway migrations, with dummy JWT and webhook secrets.
- CI (`.github/workflows/ci.yml`) runs `./mvnw -B verify` twice on every push and pull request: once on H2 and once
  against a real PostgreSQL 16 service container. A change must pass **both**; something that only works on H2
  (or only on PostgreSQL) is a bug.
- Running the app needs PostgreSQL plus `DB_PASSWORD`, `JWT_SECRET` (at least 32 characters) and
  `PAYMENT_WEBHOOK_SECRET` (at least 32 characters). See the README table for the rest, or use
  `docker compose up --build` with a `.env` copied from `.env.example`.
- The Docker files have not been run on the maintainer's machine (no Docker there). CI does not build the image.
- `target/` is build output and git-ignored. Stale reports in it (for example a `SchemaDumpTest`) are not part of the
  code; ignore them.

## Code map

`src/main/java/com/parkingSystem`:

| Package | Contents |
|---------|----------|
| `controller` | REST controllers under `/api/...`; `controller/support/Pagination` builds validated `Pageable`s |
| `service`, `service/impl` | One `IXxxService` interface per area, implemented by `XxxServiceImpl`; business rules and transactions |
| `repository` | Spring Data JPA repositories, including the `...ForUpdate` locking queries |
| `model`, `enums` | JPA entities and enums (`SpaceStatus`, `ReservationStatus`, `TicketStatus`, `SlotType`, ...) |
| `dto` | Request DTOs with Bean Validation, response DTOs with a static `from(entity)`, `PageResponse` |
| `payment` | `PaymentProvider` interface, `MockPaymentProvider`, `WebhookSigner` (HMAC), webhook receiver |
| `pricing` | `PricingCalculator` (pure, no database), `PricingProperties` (`app.pricing.*`, validated at startup) |
| `scheduling` | `ReservationExpiryJob` |
| `security` | `CurrentUserService` (who is calling, ownership checks), JSON 401/403 handlers |
| `config` | Security chain, JWT filter and util, CORS, OpenAPI, `TimeConfig` (the `Clock`), `AdminSeeder`, scheduling switch |
| `exception` | `ResourceNotFoundException` (404), `BadRequestException` (400), `ConflictException` (409), `InvalidSignatureException` (401), `GlobalExceptionHandler` |

Migrations: `src/main/resources/db/migration/V1` to `V6`. Tests: `src/test/java/com/parkingSystem/...`, with
`support/IntegrationTestSupport` (base class) and `support/MutableClock`.

## Rules the code relies on

Breaking any of these brings back a bug that was already fixed. Each is covered by tests.

1. **The schema changes only through Flyway.** Add `V<next>__description.sql`; never edit a migration that has
   already been merged. Hibernate runs with `ddl-auto=validate`, so entities and migrations must match exactly. SQL
   must run on both H2 (PostgreSQL mode) and PostgreSQL 16: no partial indexes, no PostgreSQL-only syntax.
2. **"Only one active X" rules use the active guard columns.** A nullable `active_*` column, filled by the entity
   (`@PrePersist` / `@PreUpdate`) only while the row is ACTIVE or PENDING, with a plain unique constraint. This is how
   the database refuses a second active ticket per space or vehicle, a second active reservation per space or user,
   and a second pending payment per bill. A violation must come back as a 409. See `docs/database.md`.
3. **Lock before you read, in one global order.**
   - The parking-space row (`ParkingSpaceRepository.findByIdForUpdate`) is the lock for everything that happens to
     that space. Lock it first, then read or change its tickets and reservations.
   - When two spaces are involved, lock them in ascending id order.
   - The bill row is the lock for everything that changes its payment state; lock it before the payment row.
   - Read ids with id-only queries (`findSpaceIdByReservationId`, `findSpaceIdByTicketReference`) so no entity is
     loaded, possibly stale, before its lock is held.
   - `ConcurrencyIntegrationTest` fires simultaneous requests at one space or bill and must keep giving exactly one
     winner.
4. **Time comes from the injected `Clock`.** Never call `LocalDateTime.now()` without it. Tests use `MutableClock`
   to move time forward instead of sleeping. Timestamps are truncated to microseconds, the database's precision.
5. **Responses are DTOs, lists are pages.** Controllers return `XxxDTO.from(entity)`, never an entity. Every list
   that can grow returns `PageResponse` via `Pagination.of` (size 1-100) with a fixed sort that ends with the
   primary key. `spring.jpa.open-in-view=false`, so anything a DTO reads from a lazy association must be loaded
   inside the service transaction.
6. **Authorization happens in the controller.** Admin-only methods carry `@PreAuthorize("hasRole('ADMIN')")`.
   Everything else calls `CurrentUserService.checkAccess(ownerUserId)` or `resolveUserId(requested)` (an optional
   `userId` in a request body defaults to the caller). Another user's data is a 403. The role can never be changed
   through the API. New public endpoints must be added to `SecurityConfig` on purpose.
7. **Only the payment provider settles a bill.** A bill becomes PAID only through an HMAC-signed, timestamped
   webhook (or an admin cash payment). Starting a payment requires an `Idempotency-Key`. Duplicate, late, forged and
   conflicting webhooks each have defined behaviour; see the table in `docs/architecture.md`.
8. **Pricing lives only in `PricingCalculator`.** Configuration comes from `app.pricing.*` and is validated at
   startup, so a wrong setting stops the app instead of billing customers wrongly. Billing runs inside the exit
   transaction, so a vehicle never leaves without a bill. One bill per ticket.
9. **History is kept.** Reservations are cancelled or expired, never deleted (feedback refers to them). A space
   with tickets or reservations is deactivated, not deleted. Vehicles are deactivated.
10. **Errors are JSON and say nothing internal.** Throw the custom exceptions; `GlobalExceptionHandler` turns them
    into `{timestamp, status, error, message, path}`. An unexpected error is logged and returned as a plain 500.
11. **Secrets come only from the environment.** Never commit a real password, key or `.env`. The secrets in
    `application-test.properties` and `ci.yml` are test-only dummies.

## Conventions

- Match the file you are in. Services use Lombok (`@AllArgsConstructor`, `@Slf4j`), several entities have Lombok builders.
- Most services import `jakarta.transaction.Transactional`, a few use Spring's; keep whatever the file already uses.
- Javadoc explains *why* (for example the lock order at the top of `ParkingEntryExitServiceImpl`), not *what*.
- HTTP-level tests extend `IntegrationTestSupport`: the full app on H2 with the real security chain, plus helpers
  that create users, vehicles and spaces through the API. Unit tests use Mockito. Tests are named for the behaviour
  they prove.
- Scheduling is off in tests (`app.scheduling.enabled=false`); call `ReservationExpiryJob.run()` directly.
- The H2 URL in tests sets `LOCK_TIMEOUT=10000`, which the concurrency tests depend on.

## How a change is made here

1. Start from an up-to-date `master` on a new branch. One branch and one pull request per step.
2. Write the test that fails first, then the fix. For a bug, the test must fail without the fix.
3. Schema change: a new migration, the entity, and the row in the migrations table in `docs/database.md`.
4. Keep the docs true in the same pull request: README (features, configuration, limitations), `docs/architecture.md`
   and `docs/roadmap.md`. They describe only what exists.
5. Run `./mvnw verify` before pushing. CI then proves the change on H2 and PostgreSQL.
6. Commits are authored as `manojdk`. Nothing is pushed, and no pull request is opened or merged, without the
   owner's explicit OK.

## Current state

- `master`: steps 1-5 of the review plan (Flyway, space status enum, row locks and guard columns, reservation
  lifecycle, online payments).
- PR #3, `dtos-pagination-vehicle-pricing`: steps 6-7 (DTOs everywhere, pagination, revenue as a SQL `SUM`,
  `EV_CHARGING` and `BIKE` slots, configurable pricing, migration V6). 134 tests, CI green; open until the owner
  merges it.
- Next, from `docs/roadmap.md`: process `REFUND_REQUIRED` payments and expire timed-out payments with a job;
  reservations for a future time slot (needs overlap checks against walk-ins); JWT subject by user id and refresh
  tokens; membership or permit checks for VIP and HANDICAPPED bays; a find-user-by-name-and-email endpoint.
- Known limitations are listed, on purpose, at the end of the README. Do not hide them; fix them or leave them listed.
