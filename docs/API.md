# SkyNode API

Base URL: `/api/v1`. The live schema is at `/api-docs`; Swagger UI is at `/swagger-ui.html`. `PUBLIC` means no admin login is needed. Every POST, PUT, and DELETE, including public booking requests, requires a session CSRF token from `GET /api/v1/auth/csrf`. `ADMIN` means an authenticated `ROLE_ADMIN` session is also required.

| Method | Path | Purpose | Access |
| --- | --- | --- | --- |
| GET | `/health` | Health check (outside `/api/v1`) | PUBLIC |
| GET | `/auth/csrf`, `/auth/me` | CSRF token; authentication state | PUBLIC |
| POST | `/auth/login`, `/auth/logout` | Start/end admin session | PUBLIC / authenticated session |
| GET | `/auth/session` | Earlier admin-only session endpoint | ADMIN |
| GET | `/airports`, `/airports/{id}` | Airport catalog | PUBLIC |
| POST, DELETE | `/airports`, `/airports/{id}` | Create/delete airport | ADMIN |
| GET | `/airlines`, `/airlines/{id}` | Airline catalog | ADMIN |
| POST, PUT, DELETE | `/airlines`, `/airlines/{id}` | Manage airlines | ADMIN |
| GET | `/aircraft`, `/aircraft/{id}` | Aircraft catalog | PUBLIC |
| POST, PUT, DELETE | `/aircraft`, `/aircraft/{id}` | Manage aircraft; POST/PUT require `?airlineId={id}` | ADMIN |
| GET | `/aircraft/{aircraftId}/seats`, `/aircraft/{aircraftId}/seats/{seatNumber}` | Configured aircraft seats | PUBLIC |
| POST | `/aircraft/{aircraftId}/seats` | Add seat | ADMIN |
| GET | `/flights`, `/flights/{id}` | Flight catalog | PUBLIC |
| POST, PUT, DELETE | `/flights`, `/flights/{id}` | Manage flights | ADMIN |
| GET | `/flights/search` | Search scheduled departures | PUBLIC |
| GET | `/flight-instances`, `/flight-instances/{id}`, `/flight-instances/flight/{flightId}` | Scheduled departures | PUBLIC |
| POST, PUT | `/flight-instances`, `/flight-instances/{id}` | Schedule/edit departure | ADMIN |
| PUT | `/flight-instances/{id}/status?status=DELAYED` | Change status | ADMIN |
| GET | `/bookings/{id}`, `/bookings/pnr/{pnr}`, `/bookings/passenger/{passengerName}` | Booking lookup | PUBLIC |
| POST | `/bookings` | Legacy direct booking | PUBLIC |
| PUT | `/bookings/{id}/cancel` | Cancel and release seat | PUBLIC |
| POST | `/mock-payments/start`, `/mock-payments/verify` | Demo payment draft and OTP booking | PUBLIC |

Unsupported methods and unknown routes are denied by the security rules. Local-only `/h2-console` is outside this API and requires admin login.

## Example: public booking

First, request `GET /api/v1/auth/csrf` in the same cookie session. It returns `token` and `headerName`. Send that header on the POST. The direct booking body is:

```json
{"passengerName":"Demo Passenger","flightInstanceId":1,"seatId":2}
```

`POST /api/v1/bookings` returns a booking with fields such as `id`, `pnr`, `passengerName`, `flightNumber`, `origin`, `destination`, `seatNumber`, `status`, and `bookingTimestamp`. Demo-payment bookings can also include passenger contact details, departure/arrival date-times, `totalAmount`, and `paymentStatus`.

For the UI's demo checkout, `POST /api/v1/mock-payments/start` accepts:

```json
{"fullName":"Demo Passenger","email":"demo@example.test","phone":"9876543210","age":25,"gender":"Other","flightInstanceId":1,"seatId":2}
```

It returns `paymentId`, `amount`, `currency`, and `status: "OTP_REQUIRED"`. Card number and CVV are **not** part of this request. `POST /api/v1/mock-payments/verify` accepts `{"paymentId":"<returned ID>","otp":"1234"}` and returns `{"status":"PAID_DEMO","booking":{...}}`. Any four digits are accepted; the booking is created only during verification. Repeating verification for the same session payment returns the same booking.

## Example: admin scheduling

After `POST /api/v1/auth/login` with form fields `email` and `password`, refresh the CSRF token. Then send:

```json
{"flightId":1,"departureDateTime":"2027-02-01T09:00:00","arrivalDateTime":"2027-02-01T11:00:00","status":"SCHEDULED"}
```

`POST /api/v1/flight-instances` returns `id`, `flightId`, the two date-times, and `status`. The arrival must follow departure. Supported statuses are `SCHEDULED`, `DELAYED`, `BOARDING`, `DEPARTED`, `ARRIVED`, and `CANCELLED`.

`GET /api/v1/flights/search` accepts independent optional `originAirportId`, `destinationAirportId`, and `departureDate=YYYY-MM-DD` filters. With no date, it returns scheduled records across dates. A result includes flight/airline/route names, departure/arrival times, status, and aggregate `availableSeatCount`; it does not include an instance ID or individual booked-seat states.

## Errors and privacy

Errors use `status`, `error`, `message`, `path`, and `timestamp`. Common statuses: 400 invalid input, 401 admin login required, 403 permission or CSRF failure, 404 missing record, 409 conflict (including already-booked seat), 500 sanitized unexpected failure. Public booking lookup and cancellation are based on PNR or numeric booking ID, with no passenger login. Treat PNRs as sensitive and do not use real passenger data in a public deployment.
