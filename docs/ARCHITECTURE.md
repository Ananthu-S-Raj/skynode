# SkyNode architecture

The Java API is organized by domain under `src/main/java/com/skynode`. Controllers validate HTTP input and call services. Services enforce booking and scheduling rules. Spring Data JPA repositories access the embedded H2 database. DTOs keep API JSON separate from entity graphs. Global exception handling returns consistent JSON errors.

The React/Vite application lives in `frontend/`. `App.jsx` selects hash routes, pages handle local UI state, shared components render forms/cards/tables, and `services/api.js` sends same-origin Fetch requests with the session cookie and CSRF header. Vite proxies `/api` to the backend during development.

## Data model

- `Airport` identifies an origin or destination.
- `Airline` owns aircraft and appears on flights.
- `Aircraft` has configured `Seat` records. `(aircraft_id, seat_number)` is unique.
- `Flight` ties airline, aircraft, origin, destination, and flight number together. Once an instance exists, its aircraft cannot change.
- `FlightInstance` is one scheduled departure with local departure/arrival date-times and status.
- `FlightSeat` ties one configured seat to one instance and records `AVAILABLE` or `BOOKED`. `(seat_id, flight_instance_id)` is unique.
- `Booking` references an instance and seat, has a unique PNR, and records confirmation/cancellation. Demo checkout additionally stores passenger details, fixed amount, and `PAID_DEMO`; card data and OTP are not stored.

Scheduling creates `FlightSeat` rows for seats configured at that time. Adding an aircraft seat later does not add it to already-scheduled instances.

## Booking and security

`BookingService.createBooking` checks that the instance is open and the seat belongs to its aircraft. It locks the `FlightSeat` row with `PESSIMISTIC_WRITE`, checks availability, marks it booked, and saves a booking inside a transaction. A database unique constraint protects the seat/instance pair. Cancellation locks the booking before marking it cancelled and releasing its seat; concurrent double booking and repeated cancellation are covered by integration tests.

`MockPaymentController` keeps a short-lived, session-bound draft of passenger and seat details. After a valid four-digit demo OTP, it calls the existing booking service and caches the result for repeat submissions. It does not charge a card. The legacy direct booking API remains public.

Spring Security authenticates one environment-configured BCrypt admin account with an HTTP session. Explicit request matchers allow passenger reads, booking, cancellation, and mock payment. Administrative writes require `ROLE_ADMIN`; unmatched routes are denied. CSRF applies to API writes, including public booking. The H2 console is enabled only under the local profile and has its own narrow, admin-only security chain.

## Persistence and deployment boundary

`application.properties` uses `jdbc:h2:file:./data/skynode;DB_CLOSE_ON_EXIT=FALSE` and Hibernate `ddl-auto=update`. Tests override this with isolated H2 databases. The main database is local data and is Git-ignored. The Vite proxy is only for development/preview. A static deployment needs a same-origin reverse proxy and a stronger passenger authorization design before real personal data is used.
