# Admin authentication

This file preserves the detailed authentication implementation notes and earlier
verification history. For current setup commands and the latest full-suite results,
see [README.md](README.md) and [FINAL_AUDIT_REPORT.md](FINAL_AUDIT_REPORT.md).

SkyNode uses Spring Security with one configured ADMIN account and server-side HTTP
sessions. No account table or database migration is needed. Existing domain controllers,
services, DTOs, locking and transactions are unchanged.

## Setup and run

The requested initial account has already been configured on this machine in `.env.admin`.
It contains only the email and a BCrypt cost-12 hash, has permissions `600`, and is
Git-ignored. No plaintext password is stored. Keep this file private; do not commit it,
copy it into the frontend, or `source` it (it is a Java properties file).

For a new checkout, run from the project root with Java 21 and Maven access:

```bash
./scripts/setup-admin.sh
./mvnw spring-boot:run
```

The setup prompts for email and twice for a hidden password. It refuses to overwrite an
existing `.env.admin`. To rotate credentials, move that file to a private backup outside
the repository, rerun setup, then restart the backend. Do not change database files.
Use a strong, private password before exposing this application publicly.

For an environment-only setup, use the hidden terminal prompt (Bash):

```bash
export SKYNODE_ADMIN_EMAIL=skynode@admin.com
SKYNODE_ADMIN_PASSWORD_HASH="$(./scripts/hash-admin-password.sh)" || exit 1
export SKYNODE_ADMIN_PASSWORD_HASH
./mvnw spring-boot:run
```

Enter the owner's initial password at the hidden prompt. The command stores only the hash
in the environment; it never places plaintext in command arguments, history or files.
Do not enable shell tracing (`set -x`) during credential setup. The existing private
`.env.admin` is preserved and can still be used for local startup.

Alternatively supply OS environment variables through your deployment configuration:

| Variable | Meaning |
| --- | --- |
| `SKYNODE_ADMIN_EMAIL` | Required login email |
| `SKYNODE_ADMIN_PASSWORD_HASH` | Required BCrypt hash, cost 10 or higher; not plaintext |
| `SKYNODE_SESSION_TIMEOUT` | Idle timeout, default `30m` |
| `SKYNODE_SESSION_COOKIE_SECURE` | Default `false` for local HTTP; set `true` with HTTPS |

OS environment variables override the optional local file. Startup fails when credentials
are missing or the hash format is invalid. The account is loaded on startup; sessions
are in memory and a restart requires admins to sign in again. HTTPS and a same-origin
frontend/API deployment are required when exposing the application publicly.

Run the frontend with its existing Vite proxy, then open `http://localhost:5173/#/admin/login`.
Log in using the configured account. Management controls only render after the backend
confirms an ADMIN session. The header offers Log out. Refresh checks the existing session.
The browser uses an HttpOnly, SameSite=Lax cookie named `SKYNODESESSION`; JavaScript never
stores the session ID in localStorage. Login rotates the session ID, and logout invalidates
it on the server. Login failures use a generic error for incorrect email or password.

**Restart any already-running backend yourself to activate these changes.** A process
started before the changes still runs the previous unsecured code. Do not start a second
application against the same embedded H2 file.

## Access rules

- Public: health, OpenAPI/Swagger GETs, catalog/search/seat GETs, booking lookup,
  booking creation and cancellation. Passenger workflows require no admin login.
- ADMIN only: the listed management methods under airports, airlines, aircraft (including seats),
  flights and flight instances. Direct HTTP calls enforce exactly the same permissions.
- Other routes are denied by default. New admin resources need an explicit rule.
- Missing authentication returns JSON HTTP 401. Insufficient permissions or a missing/
  expired CSRF token returns JSON HTTP 403. CSRF runs before authorization, so an anonymous
  write without a token returns 403; with a valid token it returns 401. Neither is executed.

All unsafe API requests, including public booking and cancellation, require the CSRF token
and its associated session cookie. Existing request/response DTOs and paths are unchanged;
external clients must now follow the security handshake below. Reads do not require CSRF.

### Actual endpoint inventory

Paths below are relative to `/api/v1`. No additional management methods were added.

| Resource | PUBLIC | ADMIN_ONLY |
| --- | --- | --- |
| `/airports` | GET collection and `/{id}` | POST collection; DELETE `/{id}` |
| `/airlines` | None: passenger responses already include airline names | GET collection and `/{id}`; POST collection; PUT/DELETE `/{id}` |
| `/aircraft` | GET collection and `/{id}` | POST collection; PUT/DELETE `/{id}` |
| `/aircraft/{aircraftId}/seats` | GET collection and `/{seatNumber}` | POST collection |
| `/flights` | GET collection, `/{id}`, `/search` | POST collection; PUT/DELETE `/{id}` |
| `/flight-instances` | GET collection, `/{id}`, `/flight/{flightId}` | POST collection; PUT `/{id}` and `/{id}/status` |
| `/bookings` | POST collection; GET `/{id}`, `/pnr/{pnr}`, `/passenger/{passengerName}`; PUT `/{id}/cancel` | None |

`/health` and GET OpenAPI/Swagger documentation remain public. Authentication routes are
listed below. Unknown paths/methods are denied, including unsupported administrative
PATCH requests. Public writes still require CSRF. Airline reads are administrative
because only the management UI consumes that standalone API.

## Authentication API and external clients

| Endpoint | Contract |
| --- | --- |
| `GET /api/v1/auth/csrf` | Public; returns `{token, headerName}` and initializes a session |
| `POST /api/v1/auth/login` | Form URL-encoded `email` and `password`, plus CSRF header/cookie; returns `{authenticated: true, email}` or 401 |
| `GET /api/v1/auth/me` | Public; `{authenticated: false}` for visitors, or `{authenticated: true, email, role: "ADMIN"}` for admins |
| `GET /api/v1/auth/session` | Retained ADMIN-only alias; returns authenticated state with email and role, or 401/403 |
| `POST /api/v1/auth/logout` | CSRF header/cookie; invalidates session, returns `{authenticated: false}` |

1. Fetch `/auth/csrf`, preserving its cookie. Send the returned token under `headerName`
   for every POST/PUT/DELETE, along with the cookie. No credentials are needed for public
   booking/cancellation after obtaining a token.
2. Admins POST credentials to `/auth/login`, preserving the rotated session cookie.
3. Fetch a **fresh** CSRF token after login or logout: authentication clears the old token.
4. When a write returns 403, refresh security state before the next deliberate submission.
   Never automatically retry a booking/cancellation/management write.

The frontend API module performs this handshake and clears its in-memory token after
login/logout or 403. It redirects the UI to sign-in after an admin request returns 401.
CORS was not opened; use the frontend proxy or a same-origin production reverse proxy.
Swagger remains public documentation; mutation requests there also need a valid session
and CSRF header. Existing Swagger operations were not changed to bypass security.

The H2 console stays disabled by default. The `local` profile enables it and ADMIN login
is required. Start the backend with `mvn spring-boot:run`; the Maven plugin now activates `local` by default. Override this development default with `-Dapp.profiles=other-profile` when needed.
Open `http://localhost:8080/h2-console`: an anonymous GET accepting HTML redirects to
`http://localhost:5173/#/admin/login?returnTo=h2-console`. The frontend checks the existing
session and, after successful admin login (or when already authenticated), returns to
`http://localhost:8080/h2-console`. Both destinations are fixed; arbitrary redirect URLs
are ignored. Keep Vite running and use `localhost` on both ports. Cookies are shared
across ports but not between `localhost` and `127.0.0.1`.

Console non-HTML requests and resource requests still return JSON 401 when unauthenticated;
application API 401 responses never redirect. Under non-local profiles the console remains
disabled and inaccessible, including to admins. The order-1 chain uses Boot 4.1.1
`PathRequest.toH2Console()`, exempts only the console from CSRF, and permits SAMEORIGIN
frames only there. Application CSRF protection and frame denial remain unchanged.
Never enable the local profile on a public deployment.

## Verification

Actual results on this machine:

- `./mvnw clean compile test`: BUILD SUCCESS, 28 tests, zero failures/errors/skips.
- Frontend `pnpm test`: 18 tests passed.
- Frontend `pnpm test:render`: seven route renders plus login/navigation assertions passed.
- Frontend `pnpm build`: passed, 46 modules.
- `git diff --check`: passed. Domain controllers, services, repositories, entities and DTOs match the task-start snapshot.
- Existing H2 files, including `data/skynode.mv.db`, match their task-start SHA-256 hashes.
- `.env.admin` is Git-ignored with permissions `600`. Existing uncommitted changes were preserved.


Authentication integration tests run real HTTP requests on random ports with isolated
in-memory H2 databases and a separate test-only account. They cover anonymous denial of
all management operations, invalid credentials, role enforcement, session rotation,
HttpOnly/SameSite flags, idle session expiry, invalid sessions, logout invalidation/replay, local console isolation, CSRF enforcement, and public passenger
booking/cancellation with availability counts. Existing admin/booking and concurrent
booking/cancellation tests remain in the full Maven suite. No test writes the main database.

Frontend tests cover the CSRF handshake, login form encoding, server session verification,
logout/token refresh, no automatic write retries, expired admin session recovery, ADMIN role checking, existing passenger API contracts and
static login/navigation rendering. Browser automation was declined, so interactive
login/logout and mobile visual checks still need manual verification.

Security implementation follows Spring Security's [CSRF documentation](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
and [session management documentation](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).

## Files changed for authentication

- Backend: `pom.xml`, `src/main/resources/application.properties`, and new
  `src/main/java/com/skynode/security/AdminSecurityConfig.java` and `AdminAuthController.java`.
- Setup: `.gitignore`, new `scripts/setup-admin.sh` and `scripts/SetupAdmin.java`, and `scripts/hash-admin-password.sh`;
  local ignored `.env.admin` contains only email/hash. Setup's refusal to overwrite that
  existing file was verified after compiling the helper.
- Backend tests: new `AdminAuthenticationIntegrationTest.java`, updated
  `AdminBookingApiIntegrationTest.java` and `src/test/resources/application.properties`.
- Frontend: `src/App.jsx`, `src/components/Header.jsx`, `src/components/Field.jsx`, `src/pages/Admin.jsx`, new
  `src/pages/AdminLogin.jsx`, `src/services/api.js`, `src/styles.css`,
  `tests/api.test.js` and `tests/render.mjs` (all under `frontend/`).
- Documentation: `README.md`, `frontend/README.md`, and this file.

Authentication uses Spring Boot's managed `spring-boot-starter-security`, already present when this completion pass began. No further dependencies were needed.
No new frontend dependencies, database tables, or business-service changes were introduced.
Pre-existing Git changes belong to the earlier work and were preserved.

## Remaining deployment limitations

Interactive browser refresh/login/logout and mobile checks require manual verification;
browser automation was declined. Tests use HTTP loopback, so TLS negotiation and reverse
proxy cookie behavior have not been exercised. Set `SKYNODE_SESSION_COOKIE_SECURE=true`
for HTTPS deployments, use a trusted same-origin proxy, and verify Secure cookies there.
No rate limiting or account lockout was added; use deployment-level request limits before
public exposure. Existing public booking lookup/cancellation access rules remain unchanged.

## Changes in this completion pass

The working tree already contained an initial security implementation. This pass created
`scripts/hash-admin-password.sh` and modified:

- `src/main/java/com/skynode/security/AdminSecurityConfig.java`
- `src/main/java/com/skynode/security/AdminAuthController.java`
- `src/main/java/com/skynode/config/OpenApiConfig.java` (documentation text only)
- `src/test/java/com/skynode/AdminAuthenticationIntegrationTest.java`
- `scripts/SetupAdmin.java`
- `frontend/src/App.jsx`
- `frontend/src/components/Field.jsx`
- `frontend/src/pages/AdminLogin.jsx`
- `frontend/src/services/api.js`
- `frontend/src/styles.css`
- `frontend/tests/api.test.js`
- `README.md`, `frontend/README.md`, `AUTHENTICATION.md`

Changes tighten method/path authorization, add the public `/auth/me` contract, confine
H2 exceptions to the local console, handle expired sessions without replaying writes,
refresh CSRF after login/logout, provide password visibility controls, and add focused
HTTP tests. All pre-existing tests and unrelated worktree changes were retained.
