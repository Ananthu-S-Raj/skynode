# SkyNode - Flight Booking System Backend

## Project Overview

SkyNode is a complete Spring Boot REST backend for a flight booking system. It implements a full MVC workflow from airport management through flight search and seat booking.

## Technology Stack

- **Java**: 21
- **Spring Boot**: 4.1.1
- **Maven**: Build tool
- **PostgreSQL**: Database
- **Spring Data JPA**: ORM layer
- **Hibernate**: JPA implementation
- **Springdoc-openapi**: API documentation

## Project Structure

Organized by domain module:

- `com.skynode.airport` - Airport management
- `com.skynode.airline` - Airline management
- `com.skynode.aircraft` - Aircraft management
- `com.skynode.seat` - Aircraft seat configuration
- `com.skynode.flight` - Flight management
- `com.skynode.flight.instance` - Flight instance scheduling
- `com.skynode.flight.search` - Flight search functionality
- `com.skynode.booking` - Booking and PNR management

## Prerequisites

- Java 21 JDK
- Apache Maven 3.9+
- PostgreSQL 13+ running with `skynode` database
- `SKYNODE_DB_PASSWORD` environment variable set

## Database Setup

1. Start PostgreSQL
2. Create database: `CREATE DATABASE skynode;`
3. Create user: `CREATE USER skynode_user WITH PASSWORD 'your_password';`
4. Grant privileges: `GRANT ALL PRIVILEGES ON DATABASE skynode TO skynode_user;`
5. Set the password environment variable: `export SKYNODE_DB_PASSWORD=your_password`

## Application Configuration

The application configuration is in `src/main/resources/application.properties`:

- `spring.datasource.url=jdbc:postgresql://localhost:5432/skynode`
- `spring.datasource.username=skynode_user`
- `spring.datasource.password=${SKYNODE_DB_PASSWORD}`
- `spring.jpa.hibernate.ddl-auto=update` (automatically creates/update tables)

## API Endpoint Summary

### Airport Module (`/api/v1/airports`)

- `GET /` - Get all airports
- `GET /{id}` - Get airport by ID
- `POST /` - Create airport
- `DELETE /{id}` - Delete airport

### Airline Module (`/api/v1/airlines`)

- `GET /` - Get all airlines
- `GET /{id}` - Get airline by ID
- `POST /` - Create airline
- `PUT /{id}` - Update airline
- `DELETE /{id}` - Delete airline

### Aircraft Module (`/api/v1/aircraft`)

- `GET /` - Get all aircraft
- `GET /{id}` - Get aircraft by ID
- `POST /` - Create aircraft (requires `airlineId` query param)
- `PUT /{id}` - Update aircraft (requires `airlineId` query param)
- `DELETE /{id}` - Delete aircraft

### Aircraft Seat Configuration (`/api/v1/aircraft/{id}/seats`)

- `POST /` - Add seats to aircraft (requires seat numbers and class)
- `GET /` - Get all seats for aircraft
- `GET /{seatNumber}` - Get individual seat

### Flight Module (`/api/v1/flights`)

- `GET /` - Get all flights
- `GET /{id}` - Get flight by ID
- `POST /` - Create flight (validates origin != destination)
- `PUT /{id}` - Update flight
- `DELETE /{id}` - Delete flight

### Flight Instance (`/api/v1/flight-instances`)

- `POST /` - Schedule flight instance (requires `flightId` query param)
- `GET /` - Get all flight instances
- `GET /{id}` - Get flight instance by ID
- `GET /flight/{flightId}` - Get instances for a flight
- `PUT /{id}` - Update flight instance
- `PUT /{id}/status` - Update flight instance status

### Flight Search (`/api/v1/flights/search`)

- `GET ?originAirportId=&destinationAirportId=&departureDate=` - Search flights

### Booking (`/api/v1/bookings`)

- `POST /` - Create booking (requires passengerName, flightInstanceId, seatId)
- `GET /{id}` - Get booking by ID
- `GET /pnr/{pnr}` - Get booking by PNR
- `GET /passenger/{passengerName}` - Get bookings by passenger
- `PUT /{id}/cancel` - Cancel booking

## Domain Model Relationships

```
Airport (1) -- (*) Flight
Airline (1) -- (*) Aircraft
Aircraft (1) -- (*) Seat
Aircraft (1) -- (*) FlightInstance
Flight (1) -- (*) FlightInstance
FlightInstance (1) -- (*) FlightSeat
FlightSeat (1) -- (*) Booking
Seat (1) -- (*) FlightSeat
```

## Key Business Rules

1. **Duplicate prevention**: Airline codes, aircraft registration numbers, and seat numbers within an aircraft must be unique
2. **Origin/destination validation**: Flights cannot have the same origin and destination airport
3. **Seat availability**: Seats can only be booked when AVAILABLE; booking transitions FlightSeat status to BOOKED
4. **Cancellation**: Cancelling a booking releases the seat back to AVAILABLE status
5. **PNR uniqueness**: Each booking receives a unique Passenger Name Record (8-character uppercase alphanumeric)
6. **Aircraft-seat-flight consistency**: A booking's seat must belong to the aircraft assigned to the flight instance

## Implementation Modules (in order)

1. Airport - CRUD operations with validation
2. Airline - CRUD with unique code enforcement
3. Aircraft - CRUD with airline relationship
4. Seat Configuration - Add seats to aircraft, prevent duplicate seat numbers
5. Flight - CRUD with origin/destination validation
6. FlightInstance - Schedule instances, initialize seat availability
7. Flight Search - Search by origin, destination, and date
8. Booking - Create bookings with PNR, cancel and release seats

## Build & Run

```bash
# Compile
mvn compile

# Run
mvn spring-boot:run

# Tests (requires PostgreSQL)
mvn test
```

$env:SKYNODE_TEST_DB_PASSWORD="your_actual_test_db_password"

if ($env:SKYNODE_TEST_DB_PASSWORD) { "SKYNODE_TEST_DB_PASSWORD is SET" } else { "SKYNODE_TEST_DB_PASSWORD is MISSING" }
