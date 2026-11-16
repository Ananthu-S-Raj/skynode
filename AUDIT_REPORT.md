# SkyNode backend audit

> Historical audit. See [COMPLETION_REPORT.md](COMPLETION_REPORT.md) for the subsequent
> implementation, current verification, and the remaining main-database migration blocker.

Audit date: 15 November 2026. Scope: the working tree at audit entry, including the
previous migration/refactoring changes. Earlier results were not treated as evidence:
the current wrapper build, tests, and runtime workflows were executed again.

## A. Project overview

| Item | Current inventory |
| --- | --- |
| Main Java source files | 49 |
| Java test files / test methods | 1 / 5; four existing methods retained and one added |
| Main Java packages | 36, all lowercase and matching their directory paths |
| Components | 7 controllers, 7 services, 8 repositories, 8 entities, 13 DTOs |
| Other classes | Application entry point, OpenAPI configuration, global handler, two custom exceptions, error response |
| Toolchain | Java 21; Spring Boot 4.1.1; Maven Wrapper 3.3.4 launching Maven 3.10.0 |

Domain packages remain under `com.skynode`: `airport`, `airline`, `aircraft`, `seat`,
`flight`, `flight.instance`, `flight.search`, and `booking`. Flight-seat availability
is an internal part of the seat/booking/scheduling workflow. No packages were moved.

Seven direct dependencies remain: `spring-boot-starter-webmvc`,
`spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, runtime `h2`, runtime
`spring-boot-h2console`, `springdoc-openapi-starter-webmvc-ui`, and test-scoped
`spring-boot-starter-test`. The Boot Maven plugin is the only declared build plugin.
There is no PostgreSQL dependency, duplicate direct dependency, or migration framework.
Mockito is transitive through the standard test starter; no test uses mocks and no
additional mocking configuration or direct dependency was introduced.

The main datasource remains:

```properties
spring.datasource.url=jdbc:h2:file:./data/skynode;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=false
```

`application-local.properties` enables `/h2-console` with remote access disabled.
Tests have their own `src/test/resources/application.properties`, using a randomly
named `jdbc:h2:mem:skynode_test_...` datasource and `create-drop`. The test class now also
pins that URL in `@SpringBootTest(properties=...)`, above shell environment properties,
before Hibernate initializes. Main, local, and test resource configurations are unchanged.

## B. Findings and disposition

Paths below are relative to the repository root. “Important” denotes a correctness,
compatibility, or functional-completeness issue; “optional” denotes cleanup or future
improvement. Functional probes used only a newly created audit database.

| File or area | Severity | Finding and recommendation | Disposition |
| --- | --- | --- | --- |
| `pom.xml` | Important | Springdoc 2.8.5 targets Boot 3. Align the existing library with Boot 4.1 support. | Fixed: upgraded to 3.2.0; build and OpenAPI runtime checks passed. Java and Boot versions unchanged. |
| `src/main/java/com/skynode/GlobalExceptionHandler.java` | Important | The generic handler turned framework errors such as unsupported methods/media types into 500. Preserve Spring MVC's supplied status while retaining the existing JSON error shape. | Fixed: unhandled framework `ErrorResponse` exceptions retain their status; real HTTP 405/415 and sanitized 500 verified. |
| `src/main/java/com/skynode/flight/instance/repository/FlightInstanceRepository.java` | Optional | JPQL added a wildcard even though the service already supplies the complete `%date%` pattern. Use `LIKE :date`. | Fixed; existing search behavior retained and date/route separation tested. |
| `src/test/java/com/skynode/H2BookingIntegrationTest.java` | Important | Existing fixtures checked availability but did not explicitly cover multi-seat initialization or dates/routes that should be excluded. | Fixed: one scenario added, explicit fixture initialization assertions added, both concurrent cancellation tasks now wait for a shared start. |
| Same test file | Important | A shell datasource URL can override resource configuration before Hibernate initializes, making a post-startup safety assertion too late. | Fixed: pin the in-memory URL in test properties before context startup; verified with a deliberately supplied file URL that was ignored. |
| Same test file | Optional | Repeated booking reads/streams, abbreviated future names, fully qualified types, and inconsistent imports obscured the concurrency scenario. | Fixed locally without a new test framework or helper layer. |
| `src/main/java/com/skynode/seat/Seat.java` | Important | No declared unique constraint on `(aircraft_id, seat_number)`. An isolated schema accepted two rows for the same aircraft/number. | Open correctness gap. Existing main records could not be inspected because the database is locked. No schema change proposed or applied before that inspection. |
| `src/main/java/com/skynode/seat/` | Important | Seat configuration has entities/repository operations but no controller or service API for configuring seats. | Existing functional gap, preserved and documented; no feature added silently. |
| `src/main/java/com/skynode/flight/instance/` | Important | Scheduling/get/update service operations exist, but there is no controller. | Existing functional gap; service preserved and verified directly. |
| `src/main/java/com/skynode/flight/service/FlightService.java` | Important | Changing the aircraft on an already scheduled flight returns 200, while existing flight-seat rows still reference the old aircraft. A subsequent request for an old seat returned 409. | Reproduced in isolated fixtures. Decide whether to reject reassignment or reconcile instances/seats/bookings in separate functional work. No rule changed during cleanup. |
| `src/main/java/com/skynode/flight/instance/service/FlightInstanceService.java` and `src/main/java/com/skynode/flight/dto/FlightInstanceRequestDto.java` | Important | Scheduling accepted invalid date/time text and an unknown status. Dates/statuses are unvalidated strings. | Reproduced. Define temporal/status rules before adding validation; changing column types was outside this cleanup. |
| `src/main/java/com/skynode/flight/search/service/FlightSearchService.java` | Important | Route filtering runs only when both IDs are supplied. A request with only a nonexistent origin still returned flights. | Reproduced. Choose independent filtering or requiring both parameters; existing request behavior preserved. |
| Same search service | Optional | Availability is counted once per result; eager relationships and unbounded lists can add queries as data grows. | Acceptable for current learning-project scale. Measure before introducing paging or aggregate queries. |
| `src/main/resources/application.properties` | Optional | Default Open EntityManager in View remains enabled; startup emits a warning. | Retained. Disabling it requires defining complete service read transactions and verifying API mappings. |
| Empty directories under `src/test/java/com/skynode/` | Optional | Seven empty package directories remained from earlier cleanup. | Removed only after verifying they contained no files. |
| `README.md` | Optional | Test counts and verification notes described the previous pass. | Updated to point to this report and distinguish current verification from earlier work. |

Springdoc's [official compatibility matrix](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot)
lists 3.1.x–3.2.x for Boot 4.1.x. No new direct library was added.

No duplicate fully qualified classes, package/path mismatches, generic service layers,
single-implementation service interfaces, custom factories, or unused utility classes
were found. Domain-specific DTO conversion methods are useful; replacing them with a
shared generic mapper would add complexity. Bean accessors/constructors remain for
Jackson, JPA, and existing DTO contracts. Repository/service operations for modules
without controllers were deliberately retained.

## C. Removed files and directories

**No files were removed by this audit.** All source, configuration, and documentation
files present at audit entry still exist. No tests or database files were deleted.

Only these already-empty directories were removed:

```text
src/test/java/com/skynode/flight/search/service
src/test/java/com/skynode/flight/instance/service
src/test/java/com/skynode/flight/search
src/test/java/com/skynode/flight/instance
src/test/java/com/skynode/booking/service
src/test/java/com/skynode/flight
src/test/java/com/skynode/booking
```

The following deletions were already present in the working tree before this audit and
were preserved, not performed again:

| Previously deleted file | Previously documented reason |
| --- | --- |
| `query` | Root note containing only obsolete PostgreSQL text. |
| `start` | Root note containing only obsolete PostgreSQL text. |
| `src/test/java/com/skynode/SkyNodeApplicationTests.java` | Redundant context/database safety checks consolidated into the retained integration fixture. |
| `src/test/java/com/skynode/booking/service/BookingServiceTest.java` | Mock-only workflow coverage replaced by real database integration coverage. |
| `src/test/java/com/skynode/flight/instance/service/FlightInstanceServiceTest.java` | Scheduling coverage consolidated into integration tests. |
| `src/test/java/com/skynode/flight/search/service/FlightSearchServiceTest.java` | Search coverage consolidated into integration tests. |
| `src/test/resources/application-test.properties` | Test profile indirection replaced by a single test-only `application.properties`. |

Maven wrappers and `.mvn/wrapper/maven-wrapper.properties` are necessary and retained.
`target/` is generated and ignored; `data/` contains local persistent records and is
ignored except for `.gitkeep`. No database/build artifacts are tracked by Git.
Environment-managed `.agents`, `.aws`, and `.codex` scaffolding was not altered.

## D. Simplified files

Changes relative to audit entry are deliberately small:

- `GlobalExceptionHandler`: preserves framework error statuses; existing business,
  not-found, validation, conflict, and generic error handling remains centralized.
- `FlightInstanceRepository`: removes the redundant wildcard decorator only.
- `H2BookingIntegrationTest`: clearer concurrency setup, one booking query instead
  of repeated reads, explicit seat initialization assertions, one new search scenario, and a datasource URL pinned before Hibernate initialization.
- `pom.xml`: updates only the existing Springdoc version.
- `README.md`: updates current test/verification documentation.
- `AUDIT_REPORT.md`: this requested report, newly added.

There were no dependency removals in this pass, no new architectural layers, and no
changes to controllers, DTOs, entity files, booking logic, or application database settings. Test-only URL precedence was strengthened.

## E. Architecture review

The existing controller → service → repository structure is appropriate for this
project. All seven controllers use services (health has no database responsibility);
none accesses a repository directly. Services coordinate domain rules and map the
existing response DTOs. Eight repository interfaces provide persistence without wrappers.

All 13 DTOs retain their JSON fields and contracts. No existing REST endpoint exposes
an entity to eliminate a DTO. Scheduling services return entities internally, but have
no REST controller. The two custom exception types distinguish missing resources and
business conflicts; neither is redundant. Comments are limited to state meaning and
non-obvious concurrency/foreign-key decisions.

The nine domain areas were inspected. Airports, airlines, aircraft, flights, search,
and booking have REST endpoints. Seat configuration and flight instances remain
incomplete as REST modules. Flight-seat availability works internally through scheduling,
search, booking, and cancellation.

## F. Database review

All eight entity files are byte-for-byte unchanged from audit entry. Identity primary
keys, non-null columns, relationships, and constraints were preserved. Read-only metadata
inspection of the isolated generated database confirmed:

- Eight identity-key entity tables with primary-key indexes.
- Eleven foreign keys: aircraft→airline; seat→aircraft; flight→airline, aircraft, origin,
  destination; instance→flight; flight-seat→seat/instance; booking→seat/instance.
- Unique airline code, aircraft registration, booking PNR, and flight-seat pair indexes.
- `SEATS` has only its primary-key unique index; no aircraft/seat-number unique index.

The duplicate-seat probe inserted a second same-number seat in the isolated file, then
removed only that newly created fixture. The later read-only check showed zero duplicate
groups there; this does **not** establish that duplicates are absent from the main file.

A single read-only, `IFEXISTS=TRUE`, `ACCESS_MODE_DATA=r` attempt against `data/skynode`
returned H2 error 90020 (file already in use). Access was not forced and no other process
was stopped. Existing records, constraints, and duplicate groups in the main file remain
unverified. Before any schema proposal, inspect the actual existing schema and run:

```sql
SELECT aircraft_id, seat_number, COUNT(*)
FROM seats
GROUP BY aircraft_id, seat_number
HAVING COUNT(*) > 1;
```

No DDL was executed against the main database. SHA-256 checks before and after confirmed
that **every pre-existing database and sidecar file was byte-for-byte unchanged**.

All modifying runtime checks used a new ignored file:
`data/audit-verification-9fe7ae42-00f5-4ae0-94d8-5c7f5d511fb1.mv.db`.
Separate JVM processes confirmed that both confirmed and cancelled bookings persist.
The audit file remains available locally and contains intentional negative-probe fixtures;
it is not application data. No existing H2 file was deleted or overwritten.

## G. Booking review

`BookingService`, `BookingRepository`, and `FlightSeatRepository` were unchanged.
Creation and cancellation retain `@Transactional`; flight-seat lookup retains
`PESSIMISTIC_WRITE`; cancellation retains a locked booking lookup before checking status.

For valid, uniquely configured seats, real database transactions and HTTP requests
verified one winner for two simultaneous booking attempts. Tests also verified one
successful concurrent cancellation, one conflict, seat release, rebooking, and rejection
of a repeated cancellation without releasing the replacement booking's seat.

PNRs remain eight uppercase hexadecimal characters generated from UUID text. An existence
check handles previously generated values, and the unique index remains the final
concurrency protection. An exceptional concurrent PNR collision would roll back the
transaction rather than create duplicates; collision-retry behavior was not changed.

These are controlled concurrency scenarios, not load/soak tests or a proof for direct SQL
writers. Duplicate physical seat definitions and scheduled aircraft reassignment remain
important integrity gaps, separately listed above.

## H. Test review

One test class remains: `src/test/java/com/skynode/H2BookingIntegrationTest.java`.
All four baseline tests were retained and rerun:

1. `bookingAndCancellationUpdateSearchAvailability`
2. `rejectsMissingFlightAndSeatOnAnotherAircraft`
3. `concurrentBookingAndCancellationKeepSeatConsistent`
4. `databaseEnforcesUniqueKeysAndForeignKeys`

Added: `schedulingAndSearchSeparateDatesAndRoutes`, covering two initialized seats,
independent instance availability, matching dates, reversed routes, and an absent date.
Fixture assertions now check the initialized seat ID/status, and cancellation tasks
both wait before the simultaneous start. The datasource URL is pinned before context initialization and its actual JDBC URL is
also checked before each scenario. A final build deliberately supplied a file URL in
`SPRING_DATASOURCE_URL`; all tests still used in-memory H2 and that file was not created. No failing tests were deleted, no tests were disabled, and no mocks were added.

Baseline result: four tests passed. Final result: five tests passed; zero failures,
errors, or skipped tests. The wrapper needed permission to write its distribution/cache
outside the workspace; the initial sandbox-only attempt stopped at that filesystem
restriction. Approved execution completed successfully.

## I. Verification evidence

Executed on Java 21.0.13.0.3 with Maven Wrapper / Maven 3.10.0. The plain wrapper
command passed, followed by this final run testing a deliberately conflicting shell URL:

```text
SPRING_DATASOURCE_URL='jdbc:h2:file:./data/audit-must-not-create-7d87e688;DB_CLOSE_ON_EXIT=FALSE' \
./mvnw clean compile test

Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 17.104 s
Finished at: 2026-11-15T17:55:58+05:30
```

Fresh runtime checks on the isolated persistent file:

| Check | Actual result |
| --- | --- |
| Application startup / JDBC connection | Passed; Boot 4.1.1, H2 2.4.240, file datasource |
| External datasource URL supplied during tests | Ignored; all tests used in-memory H2 and no marker database or sidecar was created |
| Airport/airline/aircraft/flight create, read, list | Passed |
| Airline/aircraft/flight update | Passed |
| Disposable CRUD deletion and subsequent GET | HTTP 200 followed by 404 for all four resources |
| Scheduling / initial seat creation | Direct service checks passed; no controller exists |
| Search / available counts | Passed for configured date/route and booking state |
| Booking / PNR / seat state after separate-JVM restart | Passed |
| Simultaneous HTTP booking | One 200, one 409 |
| Cancellation / seat release / rebooking | Passed |
| Repeated cancellation after rebooking | 409; replacement seat remained booked |
| Cancellation persisted across another JVM restart | Passed |
| Malformed JSON, missing fields, invalid ID, missing query parameter | 400 |
| Missing resource / route | 404 |
| Duplicate unique value / referenced-record deletion | 409; no SQL details returned |
| Unexpected failure | 500 with generic response; synthetic internal marker absent |
| Unsupported HTTP method / media type | 405 / 415 |
| OpenAPI after Springdoc update | 200; booking paths present |
| Local H2 console login | Passed using the isolated persistent URL |
| Default-profile console | No console servlet; URL returns 404 |

The unexpected-error check used a temporary controller registered only in the external
verification harness, not in application sources or shipped API endpoints. Runtime
negative probes reproduced the open functional findings; they are not reported as
successful business workflows.

Raw logs, dependency tree, read-only metadata output, and temporary verification sources
are under ignored `target/audit/`. They are local evidence, not committed test infrastructure;
`clean` removes them. The maintained automated suite remains reproducible with the wrapper.

## J. Remaining issues

**Critical:** no additional critical failure was reproduced for the controlled,
valid-seat booking workflows. The locked main database's data integrity could not be
assessed, so no blanket assurance is made about its records.

**Important:** missing seat-configuration and flight-instance controllers; missing
seat-pair uniqueness in the declared/generated schema; scheduled aircraft reassignment
without seat reconciliation; unvalidated scheduling strings/statuses; and ignored
single route filters. These are functional work, not cosmetic cleanup. Real-data
inspection must precede any seat-constraint schema proposal.

**Optional:** measure/paginate search as data grows; consider service read transactions
before disabling Open EntityManager in View; define supported flight/booking lifecycle
states and lock-timeout response policies as requirements evolve. Existing mutable
flight/aircraft/airline associations merit broader consistency rules before those
management APIs are used against a large booked schedule.

Compilation and these checks do not establish production readiness. Authentication,
authorization, deployment hardening, exhaustive concurrency, external PostgreSQL record
migration, and inspection of the locked main database were not part of the verified scope.
