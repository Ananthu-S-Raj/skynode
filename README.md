# SkyNode — Flight Booking System

SkyNode is a portfolio flight-booking application with a Spring Boot API, a React frontend, and a persistent, embedded H2 database. Visitors can search scheduled flights, choose a seat, complete a **demo** checkout, retrieve a booking by PNR, and cancel it. An administrator manages airports, airlines, aircraft, seats, flights, and schedules.

## Stack and structure

| Area | Technology |
| --- | --- |
| API | Java 21, Spring Boot 4.1.1, Spring Data JPA, Hibernate, Maven |
| Security | Spring Security, BCrypt, server sessions, CSRF |
| Database | File-based H2 for local use; isolated in-memory H2 for tests |
| Web | React 19, Vite 7, JavaScript, CSS, Fetch API |

The backend follows controller → service → repository → entity. `Flight` defines a route and aircraft; `FlightInstance` defines a scheduled departure. `Seat` configures an aircraft; `FlightSeat` tracks availability on a departure. See [architecture](docs/ARCHITECTURE.md) and the [API reference](docs/API.md).

## Prerequisites

- Java 21. The included Maven wrapper (`./mvnw`) downloads Maven if needed.
- Node.js 22.12 or newer and pnpm 11.25.0 for the frontend. If pnpm is missing, install that version with `npm install -g pnpm@11.25.0`.
- No PostgreSQL or external database server.

## Local setup

From the repository root:

```bash
./scripts/setup-admin.sh
./mvnw spring-boot:run
```

`setup-admin.sh` prompts for the administrator email and a hidden password, hashes it with BCrypt, and writes only the email and hash to ignored `.env.admin`. Run it once on a fresh clone; it does not overwrite an existing account. Alternatively, set `SKYNODE_ADMIN_EMAIL` and `SKYNODE_ADMIN_PASSWORD_HASH` in your environment. The application fails to start without a valid admin configuration. Never commit the hash or plaintext password. See [admin authentication](AUTHENTICATION.md).

In another terminal:

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

Open [SkyNode](http://localhost:5173). The Vite proxy sends `/api` requests to `http://127.0.0.1:8080` by default. Copy [frontend/.env.example](frontend/.env.example) to `frontend/.env.local` only when changing `VITE_API_BASE_URL` or `BACKEND_PROXY_TARGET`; keep `VITE_API_BASE_URL` relative for same-origin cookies. The API runs at [localhost:8080](http://localhost:8080/health), with [Swagger UI](http://localhost:8080/swagger-ui.html) and [OpenAPI JSON](http://localhost:8080/api-docs).

The Maven run goal activates the `local` Spring profile automatically. A packaged application does **not** inherit that profile. `local` enables the [H2 console](http://localhost:8080/h2-console), which requires the admin session; keep the frontend running on `localhost:5173` for its login redirect. Use JDBC URL `jdbc:h2:file:./data/skynode;DB_CLOSE_ON_EXIT=FALSE`, user `sa`, and an empty H2 password. Console access is disabled outside `local`.

## Booking flow

Search by route and date → choose a scheduled flight and configured seat → enter passenger details → enter correctly formatted **dummy** card fields → enter any four-digit demo OTP → receive the backend booking and PNR. The card number and CVV remain in the browser and are cleared after the demo step. No charge or SMS/email is sent. The booking is created after OTP verification; it uses a fixed demo amount of ₹4,999. PNR lookup and cancellation remain public, with CSRF protection on writes. The legacy direct `POST /api/v1/bookings` API remains available.

Individual booked seats are not exposed before checkout: the seat map shows configured seats and an aggregate available count. The backend locks the selected flight-seat row when booking and rejects a seat that is no longer available.

## Database and optional data

The main database is `data/skynode.mv.db` relative to the repository root. It persists across restarts; `spring.jpa.hibernate.ddl-auto=update` is configured. Do not delete the file. Stop the application before copying it for a backup. Database files, backups, and local credentials are Git-ignored. Tests use separate databases.

A fresh database starts empty. After creating the admin account and starting the backend, `python3 scripts/seed_demo_data.py` can add **optional** fictional airports, airlines, aircraft, seats, flights, and schedules through the existing API. It prompts for the admin password without echoing it. It does not create bookings by default. PostgreSQL records are not automatically imported.

## Tests

```bash
./mvnw clean test
cd frontend
pnpm test
pnpm test:render
pnpm build
```

The backend integration tests cover public and admin HTTP access, CSRF, H2 console policy, scheduling, seat constraints, booking, cancellation, and concurrency. The frontend tests cover API requests and static route rendering. See [FINAL_AUDIT_REPORT.md](FINAL_AUDIT_REPORT.md) for actual results and verification limits.

## Screenshots

Current application screenshots should be added to `docs/SCREENSHOTS/` only after checking that they contain no credentials, tokens, or passenger data. No reference mockups are presented as application screenshots.

## Limitations

This is a learning project, not a production booking or payment service. The payment and OTP steps are simulated. A PNR or booking ID currently grants public access to booking details and cancellation, so avoid real passenger data or public deployment without a stronger passenger authorization design. A static frontend deployment needs a same-origin `/api` proxy; the Vite proxy is for development and preview. See the [audit report](FINAL_AUDIT_REPORT.md) for other findings.
