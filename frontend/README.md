# SkyNode frontend

React 19 + Vite 7, JavaScript, plain CSS, and the Fetch API. The frontend uses the existing SkyNode Spring Boot API. See the [root README](../README.md) for backend and admin setup.

## Run

Requires Node.js 22.12+ and pnpm 11.25.0. From `frontend/`:

```bash
pnpm install --frozen-lockfile
pnpm dev
```

Open `http://localhost:5173`. The Vite development proxy forwards `/api` to `http://127.0.0.1:8080`. Copy `.env.example` to `.env.local` only to change `VITE_API_BASE_URL` or `BACKEND_PROXY_TARGET`. Keep the browser API URL relative for same-origin sessions; never put a secret in a `VITE_` variable.

```bash
pnpm test
pnpm test:render
pnpm build
pnpm preview
```

`dist/` is generated and Git-ignored. A deployed static build needs a same-origin `/api` proxy; the Vite proxy is not included in the build.

## Pages

- **Home:** airport search using backend data, plus route and destination cards derived from the flight catalog. Photos are local assets with [credits](src/assets/PHOTO_CREDITS.md).
- **Results:** real scheduled departures with route/date filters, remaining-seat counts, and sorting by departure or availability.
- **Booking:** configured-seat selection, passenger details, locally validated dummy card fields, any four-digit demo OTP, and backend-confirmed PNR.
- **My Bookings:** PNR lookup, saved booking details, and cancellation confirmation. There are no passenger accounts.
- **Admin Login and Workspace:** backend-verified session; forms and tables for airports, airlines, aircraft, seats, flights, and instances.

The API client is `src/services/api.js`. It sends the session cookie and obtains a CSRF token before all writes, including public booking. It does not store credentials or session identifiers in browser storage. Authentication state is checked with `/api/v1/auth/me` after reload. Admin APIs remain protected by the backend.

## API and UI limits

The seat API supplies aircraft configuration, not live per-seat booking status. The UI shows an aggregate available count, treats a seat selection as a preference, and displays a conflict if the backend rejects it. Search results omit instance IDs, so the frontend resolves them through catalog reads and disables an ambiguous result. No real fares, stop filters, boarding passes, or passenger account history are shown. The demo amount is the backend's fixed amount, not a ticket price.

The responsive CSS includes layouts for narrow phones, tablets, and desktop widths. Automated tests cover the API client and static rendering; they do not replace manual browser checks of keyboard use, navigation, and the complete booking flow.
