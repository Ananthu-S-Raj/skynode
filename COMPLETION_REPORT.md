# SkyNode backend completion

Completed 15 November 2026. This follow-up implements the five functional issues from
`AUDIT_REPORT.md`; it is not another repository-wide refactoring. Java 21, Boot 4.1.1,
Maven dependencies, existing endpoint paths/JSON contracts, booking transactions,
booking/flight-seat locks, cancellation protection, and existing relationships remain.

## Results and remaining database blocker

| Issue | Implementation and verification | Main database status |
| --- | --- | --- |
| Aircraft reassignment | Scheduled flights reject aircraft changes with 409 and `Cannot change aircraft after flight instances have been scheduled`. Same-aircraft updates remain allowed. Scheduling and flight updates lock the same flight row, preventing a check/schedule race. | Code implemented; isolated service and HTTP tests passed. |
| Seat uniqueness | Named `uk_aircraft_seat_number` constraint on `(aircraft_id, seat_number)`. Seat configuration serializes on the aircraft row, checks duplicates and returns 409. Another aircraft may use the same number. | **Migration pending:** main H2 file was locked; existing duplicates and actual constraint state could not be inspected. No access forced or DDL applied there. |
| REST controllers | Added seat add/list/detail and flight-instance schedule/list/detail/by-flight/update/status APIs using the existing domain objects and scheduling service. DTOs avoid exposing entities. | All new endpoints exercised on isolated H2. |
| Scheduling validation | Strict ISO local date/time parsing, arrival after departure, positive creation flight ID, and six supported status values. Updates validate the merged time interval before writing. | Direct service and HTTP checks passed. Existing records/column types not rewritten. |
| Search filters | Nullable route parameters remain optional; origin and destination are applied independently when supplied. | Origin-only, destination-only, both, no-route and unmatched-filter checks passed. |

All five code changes are implemented and verified on isolated databases. This does
**not** claim that the main database has received its new unique constraint.

## Database safety and migration

A read-only attempt against `data/skynode`, with `IFEXISTS=TRUE` and
`ACCESS_MODE_DATA=r`, returned H2 error 90020 (database already in use). The process
holding it was not stopped. Main records were not deleted, reset, recreated, copied
over, or modified. SHA-256 comparisons confirmed that every pre-existing database and
sidecar file was byte-for-byte unchanged throughout this task.

`SeatSchemaSafetyConfig` runs before Hibernate schema generation through Boot's
`HibernatePropertiesCustomizer`. For an existing `SEATS` table, it checks:

```sql
SELECT aircraft_id, seat_number
FROM seats
GROUP BY aircraft_id, seat_number
HAVING COUNT(*) > 1;
```

If duplicates exist or inspection fails, startup stops before Hibernate changes the
schema. It does not remove or normalize records. Fresh databases can create the named
constraint; a clean legacy database can acquire it through the existing `ddl-auto=update`.
No unique-constraint enforcement settings were disabled.

Two real application-context tests verified the migration paths:

- A legacy schema with duplicate seats rejected startup. Both rows remained, and
  Hibernate had not created the booking table or updated the schema.
- A clean legacy schema missing only the seat constraint acquired it on restart.
  Its existing seat survived, and a duplicate SQL insert failed with SQLState 23505.

The main migration must wait until its current owner can release the database normally.
When the updated application can open it, the preflight check inspects its current
records before Hibernate applies the constraint. If it reports duplicates, stop and
inspect those records; resolving them requires a separate data decision. Do not switch
DDL modes to `create`/`create-drop`, delete files, or bypass the guard.

No historical aircraft/seat mismatches or invalid schedule values were repaired:
main records remain uninspected while locked, and automatic reconciliation was excluded
from the requested scope.

## New REST API contracts

Existing endpoints and DTO fields were preserved. New endpoints use HTTP 200 on success,
consistent with existing non-airline creation endpoints; invalid input is 400, missing
resources 404, and conflicts 409.

| Method | Path | Request / response |
| --- | --- | --- |
| POST | `/api/v1/aircraft/{aircraftId}/seats` | Body: `seatNumber`, optional `seatClass`. Response: `id`, `aircraftId`, `seatNumber`, `seatClass`. |
| GET | `/api/v1/aircraft/{aircraftId}/seats` | List of seat responses. |
| GET | `/api/v1/aircraft/{aircraftId}/seats/{seatNumber}` | Seat response or 404. |
| POST | `/api/v1/flight-instances` | Existing request fields: `flightId`, `departureDateTime`, `arrivalDateTime`, optional `status`. `flightId` can instead be supplied in the query. |
| GET | `/api/v1/flight-instances` | List of instance responses. |
| GET | `/api/v1/flight-instances/{id}` | Instance response or 404. |
| GET | `/api/v1/flight-instances/flight/{flightId}` | Matching instance responses. |
| PUT | `/api/v1/flight-instances/{id}` | Partial time/status update. Changing the instance's flight is rejected with 400. |
| PUT | `/api/v1/flight-instances/{id}/status?status=DELAYED` | Validated status update and instance response. |

Instance responses contain `id`, `flightId`, `departureDateTime`, `arrivalDateTime`, and
`status`. If both query/body flight IDs are present, they must match; otherwise 400.

Supported statuses: `SCHEDULED`, `DELAYED`, `BOARDING`, `DEPARTED`, `ARRIVED`, `CANCELLED`.
Creation defaults to `SCHEDULED`. Dates retain the existing string representation and
must parse as ISO local date/times, for example `2027-05-15T09:00:00`. No new past-date
restriction or lifecycle-transition policy was introduced. Reads do not rewrite legacy
values; partial updates validate the complete resulting interval/status.

Seat numbers are exact, case-sensitive values. Configure seats before scheduling:
seats added later participate in subsequent instances and are not retroactively added
to existing instance inventories. Scheduling and seat creation also share an aircraft
row lock so initialization observes a consistent configuration.

Search's route IDs were nullable in the existing DTO, so optional independent filtering
was chosen. The existing `departureDate` search mechanism is unchanged; supply a
`YYYY-MM-DD` date to find scheduled instances. A supplied route filter is never ignored
because the other route parameter is absent.

## Files changed in this task

Paths below are relative to the repository root. Earlier uncommitted changes remain.

| Modified file | Purpose |
| --- | --- |
| `src/main/java/com/skynode/flight/service/FlightService.java` | Transactional reassignment restriction and locked flight lookup. |
| `src/main/java/com/skynode/flight/repository/FlightRepository.java` | Pessimistic flight-row lookup shared by updates/scheduling. |
| `src/main/java/com/skynode/flight/instance/repository/FlightInstanceRepository.java` | Existence query for scheduled instances. |
| `src/main/java/com/skynode/flight/instance/service/FlightInstanceService.java` | Shared flight/aircraft locks, schedule/update validation, instance listing. |
| `src/main/java/com/skynode/flight/search/service/FlightSearchService.java` | Independent optional filters. |
| `src/main/java/com/skynode/seat/Seat.java` | Named composite unique constraint; identifiers/relationships unchanged. |
| `src/main/java/com/skynode/seat/repository/SeatRepository.java` | Duplicate-seat existence query. |
| `src/main/java/com/skynode/aircraft/repository/AircraftRepository.java` | Aircraft-row lookup for serialized seat configuration/initialization. |
| `src/main/java/com/skynode/GlobalExceptionHandler.java` | Clear 400 response for schedule validation failures. |
| `src/test/java/com/skynode/H2BookingIntegrationTest.java` | Original five tests retained; five focused functional/concurrency tests added. |
| `README.md` | Current endpoints, examples, statuses, filters and migration instructions. |
| `AUDIT_REPORT.md` | Historical audit marked with a link to this completion report. |

| Added file | Purpose |
| --- | --- |
| `src/main/java/com/skynode/InvalidRequestException.java` | Small validation exception mapped to 400. |
| `src/main/java/com/skynode/config/SeatSchemaSafetyConfig.java` | Read-only pre-DDL duplicate check; fails closed. |
| `src/main/java/com/skynode/seat/controller/SeatController.java` | Thin seat configuration REST controller. |
| `src/main/java/com/skynode/seat/service/SeatService.java` | Seat configuration, duplicate prevention and DTO responses. |
| `src/main/java/com/skynode/seat/dto/SeatRequestDto.java` | Validated seat request. |
| `src/main/java/com/skynode/seat/dto/SeatResponseDto.java` | Flat seat response. |
| `src/main/java/com/skynode/flight/instance/controller/FlightInstanceController.java` | Thin controller for existing scheduling operations. |
| `src/main/java/com/skynode/flight/dto/FlightInstanceResponseDto.java` | Flat instance response. |
| `src/test/java/com/skynode/AdminBookingApiIntegrationTest.java` | Real HTTP admin/passenger workflows and duplicate requests. |
| `src/test/java/com/skynode/SeatSchemaSafetyTest.java` | Duplicate/clean legacy-schema migration safety tests. |
| `COMPLETION_REPORT.md` | This report. |

No files/tests were removed. No dependencies were added, removed or upgraded. Main,
local, and test datasource resource configurations are unchanged. BookingService,
BookingRepository, and FlightSeatRepository are byte-for-byte unchanged from task entry.

## Build and automated verification

Executed with Java 21 and the existing Maven wrapper:

```text
./mvnw clean compile test

AdminBookingApiIntegrationTest: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
SeatSchemaSafetyTest: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
H2BookingIntegrationTest: Tests run: 10, Failures: 0, Errors: 0, Skipped: 0

Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 26.499 s
Finished at: 2026-11-15T18:22:40+05:30
```

Focused tests also passed after each issue: reassignment, per-aircraft seat uniqueness,
scheduling validation, optional filters, then HTTP and schema-safety checks. The expected
startup-error log inside the duplicate-legacy safety test is intentional; that test passed.

The suite verifies:

- Scheduled reassignment rejected; unrelated flight-number changes and unscheduled
  reassignment accepted. Concurrent scheduling/reassignment leaves matching aircraft/seats.
- Database duplicate inserts rejected; same number on another aircraft accepted.
  Concurrent HTTP seat creation has one 200 and one 409, leaving one seat.
- New list/detail/scheduling/update/status endpoints, both flight-ID input forms,
  mismatch rejection, invalid dates/times/statuses and update rollback.
- Original booking, PNR retrieval, availability, cancellation, concurrent cancellation,
  rebooking, foreign-key/unique constraints, and scheduling initialization coverage.
- Full HTTP admin→seats→flight→schedule→search→book→retrieve→cancel→rebook workflow.
  Two simultaneous HTTP booking requests have one 200 and one 409.

Spring test contexts pin isolated in-memory URLs. Schema-safety tests start contexts
with explicit isolated memory URLs. No automated test opens the main file database.

## Persistent-file runtime verification

A new file was used, with the normal `ddl-auto=update` setting:

```text
data/completion-verification-d90ef9b2-5f4d-4cd4-841e-23445ebeb703.mv.db
```

One application process created the complete graph through HTTP, including the new seat
and scheduling APIs, then booked a seat through concurrent requests. After shutdown, a
separate JVM reopened the same file and verified saved seats, schedule, PNR, booking and
booked availability. Cancellation released the seat; rebooking succeeded; cancelling the
old booking again returned 409 without releasing the replacement booking's seat.

Metadata inspection confirmed the named composite seat unique index on both columns,
zero duplicate seat groups in the verification file, and all eleven existing foreign
keys. The constraint was present before and after restart. All pre-existing database
file hashes still matched task entry. Verification artifacts remain ignored under
`data/` and `target/completion/`; Maven clean removes only the latter.

## Unresolved items

The only requested change not applied/verified against the actual main database is its
seat-uniqueness migration and inspection of its existing records, blocked by H2 error
90020. Safe code and isolated migration behavior are verified; no claim is made about
main constraint presence, old duplicates, historical reassignment inconsistencies or
legacy invalid schedule values while the file is inaccessible. No records were repaired
or deleted automatically, and no process was terminated.

The current tests are controlled workflow/concurrency checks, not high-load testing or
a claim of production readiness. Work outside these five issues was not redesigned.
