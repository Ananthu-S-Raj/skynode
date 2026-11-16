# SkyNode final technical audit

Audit date: 16 November 2026. This report describes the current working tree, including substantial changes that were already uncommitted before this audit. No commit, push, history rewrite, main-database reset, or credential disclosure was performed.

## A. Project summary

- Java 21, Spring Boot 4.1.1, Spring Security, Spring Data JPA/Hibernate, Maven wrapper, persistent file-based H2.
- React 19, Vite 7, JavaScript, CSS, Fetch API, pnpm lockfile.
- Passenger search, seat choice, demo checkout/OTP, booking, PNR lookup/cancellation; admin catalog and schedule management.
- Backend domain packages use controllers → services → repositories → JPA entities. The frontend uses hash routes, shared components, and one API client.

## B. Code quality

**Fixed in this audit**

1. A date-omitted search previously constructed `%null%` and returned no records. It now searches across dates; an existing optional-filter integration test has a regression assertion.
2. Direct API clients could book `CANCELLED`, `DEPARTED`, or `ARRIVED` instances despite the frontend disabling them. Booking now returns a business conflict for those statuses; a regression test checks all three and verifies availability is unchanged.
3. The root and frontend READMEs previously contained stale payment descriptions, old test counts, and local-machine setup commands. They now describe the current code and reproducible setup.

The audit did not rewrite clear working services, remove tests, add frameworks, or change booking locks, cancellation logic, or PNR generation. Existing CSS includes later redesign overrides; consolidation would be optional maintenance, not a functional prerequisite.

## C. Backend audit

Controllers and route methods were inventoried against `AdminSecurityConfig` and documented in [docs/API.md](docs/API.md). DTO validation and the global exception handler provide 400/404/409 and sanitized 500 responses. Services use repositories rather than controller-side database logic. `FlightService` prevents aircraft reassignment after scheduling. `FlightInstanceService` validates ISO local date-times, arrival ordering, and supported statuses.

`BookingService` is transactional and locks the matching `FlightSeat` with `PESSIMISTIC_WRITE` before booking. Cancellation locks the booking before releasing its seat. The database has unique PNR, airline code, aircraft registration, aircraft-seat number, and flight-seat/instance constraints, plus required relationships. The integration tests exercise conflicting bookings, cancellation/rebooking, and database constraints.

## D. Frontend audit

The existing pages render and the production bundle builds. The API client uses relative `/api/v1` requests, same-origin cookies, CSRF tokens for writes, and backend messages for errors. Card number and CVV are validated in React and are not sent in the mock payment request; card state is cleared after the demo start step. Admin state is checked with the backend rather than browser storage. The seat map displays configured seats, not fabricated per-seat availability.

Responsive CSS includes mobile, tablet, and desktop breakpoints. Static rendering was verified for seven routes, and the seat-map render assertion checks that only configured seats appear. **Visual behavior at 375, 768, 1024, and 1440 pixels, keyboard navigation, and live browser flows were not rechecked in a browser**; earlier user direction declined browser automation. No current application screenshots were captured. Existing reference mockups were not presented as product screenshots.

## E. Security audit

- One environment-configured BCrypt admin account; missing/invalid configuration fails closed.
- Spring Security session login/logout, session fixation protection, HttpOnly/SameSite=Lax cookies, optional Secure cookie for HTTPS, and CSRF on unsafe API requests.
- Explicit passenger and admin request matchers; unknown routes denied. Admin management is enforced by the backend. Tests cover anonymous denial, login, logout, expiry, role checks, CSRF, and public booking.
- The local H2 console has a separate admin-only chain, narrow console CSRF exception, and SAMEORIGIN frames. It is disabled outside `local`.

**Critical remaining privacy risk:** `GET /api/v1/bookings/{id}`, `GET /api/v1/bookings/pnr/{pnr}`, `GET /api/v1/bookings/passenger/{passengerName}`, and `PUT /api/v1/bookings/{id}/cancel` are public. A PNR or guessable numeric ID can expose saved passenger details or permit cancellation. This is an existing product contract, so it was not silently changed during a portfolio audit. Do not use real passenger data or publicly deploy without a passenger authorization redesign. Demo card/OTP behavior is not real payment security.

**Critical Git history finding:** Commit `4fd218b1` contains a nonempty literal datasource password in historical `src/main/resources/application.properties` and `src/test/resources/application-test.properties`. The current working tree uses an empty H2 password and ignored admin configuration, but Git history remains accessible after publication. Treat the historical credential as compromised: rotate it if still used anywhere, then plan an explicit history-remediation workflow before publishing this repository. This audit did not print the value or rewrite history.

A scoped worktree scan found only test fixtures, setup instructions, and script variable references as other credential-pattern candidates; no current plaintext admin secret, card number, session token, or CSRF token was identified in source. The ignored `.env.admin` file was not read or staged.

## F. Database audit

The main datasource is `jdbc:h2:file:./data/skynode;DB_CLOSE_ON_EXIT=FALSE`, with `ddl-auto=update`. The local test datasource is isolated in-memory H2. `SeatSchemaSafetyConfig` checks legacy seat duplicates before Hibernate attempts the new unique constraint. Integration tests verify unique/foreign keys and clean/duplicate legacy-schema behavior.

A separate temporary file-based H2 database was used for a startup → admin setup → search → public demo booking → stop → restart → PNR lookup → cancellation check. The database file was created and the booking persisted across restart. The **main** `data/skynode.mv.db` was not opened or modified by this audit; its current records and constraint state remain unverified here.

## G. Actual test and runtime results

| Check | Result |
| --- | --- |
| `./mvnw clean test` after fixes | **30 tests, 0 failures/errors/skips; BUILD SUCCESS** |
| Frontend unit tests | **19 passed, 0 failed** |
| Frontend static render | **7 routes plus login/navigation and seat-map assertions passed** |
| Vite production build | **Passed; 49 modules transformed** |
| `pnpm audit --json` | **0 reported info/low/moderate/high/critical advisories** |
| Live `/health` | **HTTP 200** |
| Live anonymous `/h2-console` | **HTTP 401**; expected denial, not proof of an authenticated live console session |
| Isolated temporary app | **Admin management, public search, demo booking, repeated verification, file persistence, PNR lookup, and cancellation passed** |
| Isolated Maven `spring-boot:run` | **Started with `local` profile and H2 console registration in startup log** |

The backend tests also cover direct HTTP login/logout, public booking and cancellation, seat conflicts, concurrent booking, scheduling validation, and admin denial. A Maven-specific dependency advisory scan was not run; the pnpm advisory result does not cover Java dependencies. The live main H2 database and real local administrator account were not used for destructive checks.

## H. Documentation

Updated [README.md](README.md), [frontend/README.md](frontend/README.md), and [AUTHENTICATION.md](AUTHENTICATION.md). Added [docs/API.md](docs/API.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), and a screenshot safety note under `docs/SCREENSHOTS/`. The API guide derives methods and paths from controllers; the architecture guide explains the entity distinctions and transactions. No screenshot from the current redesigned app was available for safe inclusion.

## I. GitHub readiness

- Branch: `main`. Existing remote: `origin` pointing to `git@github.com:Ananthu-S-Raj/skynode.git`; it was not changed.
- The initial working tree was not clean: backend files were modified, some older tracked tests/files were deleted, and the frontend plus new tests/docs were untracked. These pre-existing changes were preserved.
- `.gitignore` covers Maven outputs, frontend dependencies/builds, local environment files, logs, IDE/system files, and H2 data/backups. `frontend/.env.example` is not ignored. `git ls-files` showed no tracked database, env, build-output, or log files.
- Maven wrapper scripts/properties and the pnpm lockfile are present. A fresh clone can set up an admin, start an empty H2 database, install frontend dependencies, and optionally seed fictional data through the API.
- **Not ready to publish yet:** the historical datasource password needs rotation and a reviewed Git-history remediation plan. The public booking privacy design also blocks use with real passenger data.

## J. Final status

| Priority | Finding | Status |
| --- | --- | --- |
| Critical | Historical committed datasource password | **Remaining**; rotate and review history remediation before public release |
| Critical | Public booking lookup/cancellation exposes personal information by PNR or ID | **Remaining**; existing API contract, requires product/security redesign |
| Important | Date-omitted search returned no records | **Fixed and tested** |
| Important | Direct booking accepted closed departures | **Fixed and tested** |
| Important | Main persistent H2 records/constraint state | **Unverified**; isolated databases tested, main data preserved |
| Important | Reproducible setup and documentation | **Updated and reviewed** |
| Optional | Current UI screenshots and manual viewport/browser check | **Unverified** |
| Optional | Maven dependency advisory scan and CSS override consolidation | **Remaining** |

Suggested commit message after review and security remediation: `Prepare SkyNode for public GitHub release`. No commit or push was made.
