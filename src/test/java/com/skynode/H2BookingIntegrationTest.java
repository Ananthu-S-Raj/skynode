package com.skynode;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;
import com.skynode.airport.entity.Airport;
import com.skynode.airport.repository.AirportRepository;
import com.skynode.booking.dto.BookingRequestDto;
import com.skynode.booking.dto.BookingResponseDto;
import com.skynode.booking.entity.Booking;
import com.skynode.booking.repository.BookingRepository;
import com.skynode.booking.service.BookingService;
import com.skynode.flight.dto.FlightInstanceRequestDto;
import com.skynode.flight.dto.FlightRequestDto;
import com.skynode.flight.entity.Flight;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.flight.instance.service.FlightInstanceService;
import com.skynode.flight.repository.FlightRepository;
import com.skynode.flight.search.dto.FlightSearchRequestDto;
import com.skynode.flight.search.service.FlightSearchService;
import com.skynode.flight.service.FlightService;
import com.skynode.seat.FlightSeat;
import com.skynode.seat.Seat;
import com.skynode.seat.dto.SeatRequestDto;
import com.skynode.seat.repository.FlightSeatRepository;
import com.skynode.seat.repository.SeatRepository;
import com.skynode.seat.service.SeatService;

import static org.junit.jupiter.api.Assertions.*;

// Pin the URL before Hibernate starts, even when the shell supplies a datasource URL.
@SpringBootTest(properties =
    "spring.datasource.url=jdbc:h2:mem:skynode_test_${random.uuid};DB_CLOSE_ON_EXIT=FALSE")
class H2BookingIntegrationTest {
    @Autowired
    AirlineRepository airlines;

    @Autowired
    AirportRepository airports;

    @Autowired
    AircraftRepository aircraft;

    @Autowired
    SeatRepository seats;

    @Autowired
    SeatService seatService;

    @Autowired
    FlightRepository flights;

    @Autowired
    FlightSeatRepository flightSeats;

    @Autowired
    BookingRepository bookings;

    @Autowired
    FlightInstanceService scheduling;

    @Autowired
    FlightSearchService search;

    @Autowired
    FlightService flightService;

    @Autowired
    BookingService booking;

    @Autowired
    DataSource dataSource;

    private String suffix;
    private Airline airline;
    private Aircraft plane;
    private Seat seat;
    private FlightInstance instance;
    private FlightSearchRequestDto query;
    private BookingRequestDto request;

    @BeforeEach
    void setUp() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:skynode_test_"));
        }
        suffix = UUID.randomUUID().toString();
        airline = airlines.saveAndFlush(new Airline("H2 Test", suffix));
        Airport origin = airports.saveAndFlush(new Airport("Origin", "India", "Kochi"));
        Airport destination = airports.saveAndFlush(new Airport("Destination", "India", "Delhi"));
        plane = aircraft.saveAndFlush(new Aircraft(airline, "A320", suffix));
        seat = seats.saveAndFlush(new Seat(plane, "1A", "ECONOMY"));
        Flight flight = flights.saveAndFlush(new Flight(airline, origin, destination, plane, suffix));
        FlightInstanceRequestDto schedule = new FlightInstanceRequestDto();
        schedule.setFlightId(flight.getId());
        schedule.setDepartureDateTime("2027-01-15T09:00:00");
        schedule.setArrivalDateTime("2027-01-15T11:00:00");
        instance = scheduling.scheduleFlightInstance(schedule);
        List<FlightSeat> initializedSeats = flightSeats.findByFlightInstance(instance);
        assertEquals(1, initializedSeats.size());
        assertEquals(seat.getId(), initializedSeats.getFirst().getSeat().getId());
        assertEquals("AVAILABLE", initializedSeats.getFirst().getStatus());
        query = new FlightSearchRequestDto();
        query.setOriginAirportId(origin.getId());
        query.setDestinationAirportId(destination.getId());
        query.setDepartureDate("2027-01-15");
        request = new BookingRequestDto("H2 Passenger " + suffix, instance.getId(), seat.getId());
    }

    @Test
    void bookingAndCancellationUpdateSearchAvailability() {
        assertEquals(1L, search.search(query).getFirst().getAvailableSeatCount());
        BookingResponseDto first = booking.createBooking(request);
        assertTrue(first.getPnr().matches("[A-Z0-9]{8}"));
        assertEquals(first.getId(), booking.getBookingByPnr(first.getPnr()).getId());
        assertEquals(first.getPnr(), booking.getBookingById(first.getId()).getPnr());
        assertEquals(0L, search.search(query).getFirst().getAvailableSeatCount());
        assertThrows(BusinessConflictException.class, () -> booking.createBooking(request));
        assertEquals("CANCELLED", booking.cancelBooking(first.getId()).getStatus());
        assertEquals(1L, search.search(query).getFirst().getAvailableSeatCount());
        assertThrows(BusinessConflictException.class, () -> booking.cancelBooking(first.getId()));
    }

    @Test
    void rejectsMissingFlightAndSeatOnAnotherAircraft() {
        assertThrows(ResourceNotFoundException.class,
            () -> booking.createBooking(new BookingRequestDto("Missing", Long.MAX_VALUE, seat.getId())));
        Aircraft otherPlane = aircraft.saveAndFlush(new Aircraft(airline, "A320", suffix + "-OTHER"));
        Seat wrongSeat = seats.saveAndFlush(new Seat(otherPlane, "2A", "ECONOMY"));
        assertThrows(BusinessConflictException.class,
            () -> booking.createBooking(new BookingRequestDto("Wrong aircraft", instance.getId(), wrongSeat.getId())));
    }

    @Test
    void concurrentBookingAndCancellationKeepSeatConsistent() throws Exception {
        // Real transactions on separate threads/connections, with a simultaneous start.
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt = () -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                try {
                    booking.createBooking(request);
                    return true;
                } catch (BusinessConflictException expected) {
                    return false;
                }
            };
            var firstAttempt = executor.submit(attempt);
            var secondAttempt = executor.submit(attempt);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(firstAttempt.get(15, TimeUnit.SECONDS), secondAttempt.get(15, TimeUnit.SECONDS));
        }
        List<Booking> passengerBookings = bookings.findByPassengerName(request.getPassengerName());
        assertEquals(1, passengerBookings.size());
        Booking confirmed = passengerBookings.getFirst();
        assertEquals("CONFIRMED", confirmed.getStatus());
        assertEquals(0L, search.search(query).getFirst().getAvailableSeatCount());

        CountDownLatch cancelReady = new CountDownLatch(2);
        CountDownLatch cancelStart = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> cancel = () -> {
                cancelReady.countDown();
                assertTrue(cancelStart.await(10, TimeUnit.SECONDS));
                try {
                    booking.cancelBooking(confirmed.getId());
                    return true;
                } catch (BusinessConflictException expected) {
                    return false;
                }
            };
            var firstAttempt = executor.submit(cancel);
            var secondAttempt = executor.submit(cancel);
            assertTrue(cancelReady.await(10, TimeUnit.SECONDS));
            cancelStart.countDown();
            assertNotEquals(firstAttempt.get(15, TimeUnit.SECONDS), secondAttempt.get(15, TimeUnit.SECONDS));
        }
        booking.createBooking(request);
        assertThrows(BusinessConflictException.class, () -> booking.cancelBooking(confirmed.getId()));
        assertEquals(0L, search.search(query).getFirst().getAvailableSeatCount());
    }

    @Test
    void schedulingAndSearchSeparateDatesAndRoutes() {
        seats.saveAndFlush(new Seat(plane, "1B", "ECONOMY"));
        Flight flight = instance.getFlight();
        FlightInstance nextDay = scheduling.scheduleFlightInstance(new FlightInstanceRequestDto(
            flight.getId(), "2027-01-16T09:00:00", "2027-01-16T11:00:00", "SCHEDULED"
        ));
        assertEquals(2, flightSeats.findByFlightInstance(nextDay).size());
        assertEquals(2L, flightSeats.countByFlightInstanceAndStatus(nextDay, "AVAILABLE"));
        assertEquals(1L, flightSeats.countByFlightInstanceAndStatus(instance, "AVAILABLE"));

        query.setDepartureDate("2027-01-16");
        var results = search.search(query);
        assertEquals(1, results.size());
        assertEquals("2027-01-16T09:00:00", results.getFirst().getDepartureTime());
        assertEquals(2L, results.getFirst().getAvailableSeatCount());

        query.setOriginAirportId(flight.getDestination().getId());
        query.setDestinationAirportId(flight.getOrigin().getId());
        assertTrue(search.search(query).isEmpty());
        query.setOriginAirportId(flight.getOrigin().getId());
        query.setDestinationAirportId(flight.getDestination().getId());
        query.setDepartureDate("2027-01-17");
        assertTrue(search.search(query).isEmpty());
    }

    @Test
    void scheduledFlightsKeepTheirAircraftButAllowOtherUpdates() {
        Flight flight = instance.getFlight();
        Aircraft other = aircraft.saveAndFlush(new Aircraft(airline, "A321", suffix + "-NEW"));
        FlightRequestDto update = new FlightRequestDto(airline.getId(), flight.getOrigin().getId(),
            flight.getDestination().getId(), other.getId(), "CHANGED");
        assertThrows(BusinessConflictException.class, () -> flightService.updateFlight(flight.getId(), update));
        assertEquals(plane.getId(), flights.findById(flight.getId()).orElseThrow().getAircraft().getId());

        update.setAircraftId(plane.getId());
        assertEquals("CHANGED", flightService.updateFlight(flight.getId(), update).getFlightNumber());
        Flight unscheduled = flights.saveAndFlush(new Flight(airline, flight.getOrigin(),
            flight.getDestination(), plane, suffix + "-UNSCHEDULED"));
        update.setAircraftId(other.getId());
        flightService.updateFlight(unscheduled.getId(), update);
        assertEquals(other.getId(), flights.findById(unscheduled.getId()).orElseThrow().getAircraft().getId());
    }

    @Test
    void seatNumbersAreUniquePerAircraft() {
        assertThrows(DataIntegrityViolationException.class,
            () -> seats.saveAndFlush(new Seat(plane, "1A", "ECONOMY")));
        assertThrows(BusinessConflictException.class,
            () -> seatService.addSeat(plane.getId(), new SeatRequestDto("1A", "ECONOMY")));
        Aircraft other = aircraft.saveAndFlush(new Aircraft(airline, "A321", suffix + "-OTHER"));
        var created = seatService.addSeat(other.getId(), new SeatRequestDto("1A", "ECONOMY"));
        assertEquals("1A", created.seatNumber());
        assertEquals(1, seatService.getSeats(other.getId()).size());
        assertEquals(created.id(), seatService.getSeat(other.getId(), "1A").id());
    }

    @Test
    void schedulesAndUpdatesRejectInvalidTimesAndStatuses() {
        Long flightId = instance.getFlight().getId();
        for (String invalid : List.of("2027-02-30T09:00:00", "2027-01-15", "2027-01-15T25:00:00", "")) {
            assertThrows(InvalidRequestException.class, () -> scheduling.scheduleFlightInstance(
                new FlightInstanceRequestDto(flightId, invalid, "2027-03-01T10:00:00", "SCHEDULED")));
        }
        assertThrows(InvalidRequestException.class, () -> scheduling.scheduleFlightInstance(
            new FlightInstanceRequestDto(flightId, "2027-01-15T10:00:00", "2027-01-15T09:00:00", "SCHEDULED")));
        assertThrows(InvalidRequestException.class, () -> scheduling.scheduleFlightInstance(
            new FlightInstanceRequestDto(flightId, "2027-01-15T09:00:00", "2027-01-15T10:00:00", "UNKNOWN")));
        assertThrows(InvalidRequestException.class, () -> scheduling.updateFlightStatus(instance.getId(), "UNKNOWN"));
        FlightInstanceRequestDto update = new FlightInstanceRequestDto();
        update.setArrivalDateTime("2027-01-15T08:00:00");
        assertThrows(InvalidRequestException.class, () -> scheduling.updateFlightInstance(instance.getId(), update));
        assertEquals("2027-01-15T11:00:00", scheduling.getInstanceById(instance.getId()).getArrivalDateTime());
        update.setArrivalDateTime("2027-01-15T12:00:00");
        assertEquals("2027-01-15T12:00:00", scheduling.updateFlightInstance(instance.getId(), update).getArrivalDateTime());
        scheduling.updateFlightStatus(instance.getId(), "DELAYED");
        assertEquals("DELAYED", scheduling.getInstanceById(instance.getId()).getStatus());
    }

    @Test
    void searchAppliesEachOptionalRouteFilter() {
        Flight flight = instance.getFlight();
        query.setDestinationAirportId(null);
        assertEquals(1, search.search(query).size());
        query.setOriginAirportId(Long.MAX_VALUE);
        assertTrue(search.search(query).isEmpty());
        query.setOriginAirportId(null);
        query.setDestinationAirportId(flight.getDestination().getId());
        assertEquals(1, search.search(query).size());
        query.setDestinationAirportId(Long.MAX_VALUE);
        assertTrue(search.search(query).isEmpty());
        query.setDestinationAirportId(null);
        assertTrue(search.search(query).stream().anyMatch(result ->
            result.getFlightNumber().equals(flight.getFlightNumber())));
        query.setDepartureDate(null);
        assertTrue(search.search(query).stream().anyMatch(result ->
            result.getFlightNumber().equals(flight.getFlightNumber())));
    }

    @Test
    void directBookingRejectsClosedDepartures() {
        for (String status : List.of("CANCELLED", "DEPARTED", "ARRIVED")) {
            scheduling.updateFlightStatus(instance.getId(), status);
            assertThrows(BusinessConflictException.class, () -> booking.createBooking(request));
            assertEquals(1L, search.search(query).getFirst().getAvailableSeatCount());
        }
    }

    @Test
    void schedulingAndAircraftUpdatesCannotLeaveMismatchedSeats() throws Exception {
        Flight original = instance.getFlight();
        Flight unscheduled = flights.saveAndFlush(new Flight(airline, original.getOrigin(),
            original.getDestination(), plane, suffix + "-RACE"));
        Aircraft other = aircraft.saveAndFlush(new Aircraft(airline, "A321", suffix + "-RACE"));
        seats.saveAndFlush(new Seat(other, "1A", "ECONOMY"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var scheduled = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return scheduling.scheduleFlightInstance(new FlightInstanceRequestDto(unscheduled.getId(),
                    "2027-06-01T09:00:00", "2027-06-01T11:00:00", "SCHEDULED"));
            });
            var updated = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                try {
                    flightService.updateFlight(unscheduled.getId(), new FlightRequestDto(airline.getId(),
                        original.getOrigin().getId(), original.getDestination().getId(), other.getId(), "RACE"));
                    return true;
                } catch (BusinessConflictException expected) {
                    return false;
                }
            });
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            FlightInstance result = scheduled.get(15, TimeUnit.SECONDS);
            updated.get(15, TimeUnit.SECONDS);
            Long assignedAircraft = flights.findById(unscheduled.getId()).orElseThrow().getAircraft().getId();
            var initialized = flightSeats.findByFlightInstance(result);
            assertEquals(1, initialized.size());
            assertEquals(assignedAircraft, initialized.getFirst().getSeat().getAircraft().getId());
        }
    }

    @Test
    void databaseEnforcesUniqueKeysAndForeignKeys() {
        assertThrows(DataIntegrityViolationException.class,
            () -> airlines.saveAndFlush(new Airline("Duplicate", suffix)));
        assertThrows(DataIntegrityViolationException.class,
            () -> flightSeats.saveAndFlush(new FlightSeat(seat, instance, "AVAILABLE")));
        // FK enforcement is independent of application-level validation.
        Aircraft missing = new Aircraft();
        missing.setId(Long.MAX_VALUE);
        assertThrows(DataIntegrityViolationException.class,
            () -> seats.saveAndFlush(new Seat(missing, "99A", "ECONOMY")));
    }
}
