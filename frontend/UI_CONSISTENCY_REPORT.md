# Shared navy heroes and authenticated H2 navigation

## Implemented UI

Flights retains its existing design and search behavior. My Bookings, Admin Login
and Admin Workspace now use the same navy hero background, heading typography and outer container as Flights.
The PNR form, centered narrow login card, and admin workspace content overlap the hero edge by 32px. Cards retain
shared white surfaces, borders, radius and subtle shadows. Existing shared header/footer, shared admin DataTable,
form fields, booking/cancellation behavior and authentication requests are preserved.
No dependencies or styling libraries were added.

## H2 cause and corrected flow

The earlier 401 was the configured unauthenticated security response, not proof that the
console servlet was enabled. The reported screenshots also showed a signed-out state
and a localhost/127.0.0.1 mismatch. The existing default profile disables H2 Console;
only `local` enables it. A host-only session cookie is shared across ports on localhost,
but not between localhost and 127.0.0.1. Vite's existing `/api` proxy remains unchanged.

The higher-priority, local-only chain uses Boot 4.1.1 `PathRequest.toH2Console()` and still
requires ROLE_ADMIN. Only console requests are exempt from CSRF and permit SAMEORIGIN
frames. Application API authorization, CSRF, JSON errors and cookie protections remain.

1. Browser GET `/h2-console` or `/h2-console/` accepting HTML while anonymous redirects
   to `http://localhost:5173/#/admin/login?returnTo=h2-console`.
2. The existing login verifies credentials, rotates the session, checks `/api/v1/auth/me`
   and refreshes CSRF as before. Once an ADMIN session is confirmed, the frontend returns
   to `http://localhost:8080/h2-console`. Already authenticated visits to the marked login
   route return there too.
3. Both redirect destinations are fixed. No request parameter can choose a different
   URL, host or protocol. Other returnTo values are ignored.
4. H2 itself redirects its bare servlet URL to `/h2-console/`, then displays its connection
   form. After logout, browser entry requires login again; unauthenticated console resources
   receive JSON 401. API requests never redirect to the frontend.
5. Outside `local`, the console remains disabled and inaccessible, even for admins.

## Run locally

Stop your current backend normally and start one instance from the project root:

```bash
mvn spring-boot:run
```

Keep Vite running at `http://localhost:5173`. Open `http://localhost:8080/h2-console`.
The login round trip uses localhost on both ports. The console's separate database form
uses driver `org.h2.Driver`, URL `jdbc:h2:file:./data/skynode;DB_CLOSE_ON_EXIT=FALSE`,
username `sa` and the existing blank database password. Database credentials are not
placed in React. Do not start another backend against the same embedded file, delete
locks or reset the database. Never enable `local` on a public deployment.

## Files changed

- `frontend/src/pages/MyBookings.jsx`
- `frontend/src/pages/AdminLogin.jsx`
- `frontend/src/pages/Admin.jsx`
- `frontend/src/App.jsx`
- `frontend/src/styles.css`
- `frontend/tests/render.mjs`
- `src/main/java/com/skynode/security/AdminSecurityConfig.java`
- `src/test/java/com/skynode/AdminAuthenticationIntegrationTest.java`
- `README.md`, `AUTHENTICATION.md`, this report

## Verification and limits

Actual results: `./mvnw clean compile test` succeeded with 28 tests, zero failures/errors/skips.
The production frontend build succeeded (46 modules); all 18 frontend API tests and seven
static route checks passed, including shared heroes and fixed return-destination checks. HTTP coverage includes fixed HTML
redirects, unchanged API JSON 401, login and ADMIN state, H2's own trailing-slash redirect,
console resources/frames, connection to the existing isolated test database, logout,
hostname cookie scope, non-local denial, public booking/cancellation and concurrency.
Tests use isolated databases and do not connect to the main persistent H2 file.

Static frontend checks cover all routes, shared heroes and rejection of arbitrary redirect
URLs. The four supplied screenshots were reviewed as references. Interactive browser
navigation and visual checks at 375/768/1024/1440px remain unverified because browser
automation was previously declined. The existing live port-8080 process was not restarted
or authenticated on the owner's behalf. Its main persistent database connection has not
been exercised by this work; it must be checked after local startup and browser login.

Database hash check for this task: all files match the task-start snapshot.

A tab becoming visible now checks `/api/v1/auth/me` and refreshes its navigation from the backend. This addresses stale login/logout displays across tabs without storing credentials or session state in browser storage. The admin resource tables already use one `DataTable` component with consistent headers, borders, mobile row layout, loading and empty states; no second table implementation was added. Flight results remain route cards because their time and destination layout is clearer than a wide table.

## Maven development profile and frontend refinement

The existing Spring Boot Maven plugin uses `<profiles>${app.profiles}</profiles>` with `app.profiles=local` in `pom.xml`. This affects the Maven run goal only; tests and packaged applications retain their own profile selection. An override is available with `-Dapp.profiles=other-profile`. The UI audit kept the shared navy hero and card system on the existing pages, added it to Admin Workspace, and standardized form/table spacing and table text sizes. Admin tables continue to use the shared `DataTable`; flight results keep route cards. Tabs now check the backend session when they become visible, so navigation catches up after login or logout elsewhere.

Verification for this pass: `mvn clean compile test` passed (28 tests); 18 frontend tests and seven static route checks passed; Vite production build passed (46 modules). `mvn spring-boot:run` logged active profile `local` while pointed at a temporary H2 file on port 18080. An anonymous real HTTP request to `/h2-console` returned 302 to the fixed localhost login route. The temporary backend was stopped after verification. The live port-8080 console and responsive browser widths were not checked.
