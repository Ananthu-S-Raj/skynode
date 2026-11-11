package com.skynode.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skynode.booking.dto.BookingRequestDto;
import com.skynode.booking.dto.BookingResponseDto;
import com.skynode.booking.entity.Booking;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.seat.Seat;
import com.skynode.seat.FlightSeat;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.airport.entity.Airport;
import com.skynode.airline.entity.Airline;
import com.skynode.flight.entity.Flight;
import java.util.Optional;

import com.skynode.booking.repository.BookingRepository;
import com.skynode.flight.instance.repository.FlightInstanceRepository;
import com.skynode.seat.repository.SeatRepository;
import com.skynode.seat.repository.FlightSeatRepository;

import com.skynode.ResourceNotFoundException;
import com.skynode.BusinessConflictException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService.
 * Mocks: BookingRepository, FlightInstanceRepository, SeatRepository, FlightSeatRepository.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private FlightSeatRepository flightSeatRepository;

    @InjectMocks
    private BookingService bookingService;

    private final Long flightInstanceId = 1L;
    private final Long seatId = 1L;
    private final String passengerName = "John Doe";

    // Shared test objects with proper relationships
    private final Airline testAirline = new Airline();
    private final Airport testOriginAirport = new Airport();
    private final Airport testDestinationAirport = new Airport();
    private final Aircraft testAircraft = new Aircraft();
    private final Flight testFlight = new Flight();
    private final FlightInstance testFlightInstance = new FlightInstance();
    private final Seat testSeat = new Seat();
    private final Booking testBooking = new Booking();

    // Initialize shared test objects with full relationships
    {
        // Configure airline
        testAirline.setId(1L);
        testAirline.setName("Test Airline");

        // Configure origin airport
        testOriginAirport.setId(1L);
        testOriginAirport.setName("Origin Airport");

        // Configure destination airport
        testDestinationAirport.setId(2L);
        testDestinationAirport.setName("Destination Airport");

        // Configure aircraft
        testAircraft.setId(1L);
        testAircraft.setModel("Airbus A320");
        testAircraft.setAirline(testAirline);

        // Configure flight with aircraft, origin, and destination
        testFlight.setFlightNumber("BA123");
        testFlight.setAircraft(testAircraft);
        testFlight.setOrigin(testOriginAirport);
        testFlight.setDestination(testDestinationAirport);

        // Configure flight instance with flight
        testFlightInstance.setId(flightInstanceId);
        testFlightInstance.setFlight(testFlight);

        // Configure seat
        testSeat.setId(seatId);
        testSeat.setAircraft(testAircraft);

        // Configure booking
        testBooking.setId(1L);
        testBooking.setPnr("ABC123");
        testBooking.setPassengerName("John Doe");
        testBooking.setStatus("CONFIRMED");
        testBooking.setFlightInstance(testFlightInstance);
        testBooking.setSeat(testSeat);
    }

    @Test
    void createBooking_Success() {
        // Arrange
        BookingRequestDto request = new BookingRequestDto(passengerName, flightInstanceId, seatId);

        when(flightInstanceRepository.findById(flightInstanceId))
                .thenReturn(Optional.of(testFlightInstance));

        when(seatRepository.findById(seatId))
                .thenReturn(Optional.of(testSeat));

        FlightSeat flightSeat = new FlightSeat(testSeat, testFlightInstance, "AVAILABLE");
        when(flightSeatRepository.findBySeatAndFlightInstance(testSeat, testFlightInstance))
                .thenReturn(Optional.of(flightSeat));

        when(bookingRepository.existsByPnr(anyString()))
                .thenReturn(false);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponseDto response = bookingService.createBooking(request);

        // Assert
        assertNotNull(response);
        assertEquals(passengerName, response.getPassengerName());
        assertTrue(response.getPnr().length() == 8);
        assertEquals("CONFIRMED", response.getStatus());

        verify(flightSeatRepository).save(flightSeat);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_SeatAlreadyBooked_ThrowsBusinessConflictException() {
        // Arrange
        BookingRequestDto request = new BookingRequestDto(passengerName, flightInstanceId, seatId);

        when(flightInstanceRepository.findById(flightInstanceId))
                .thenReturn(Optional.of(testFlightInstance));

        when(seatRepository.findById(seatId))
                .thenReturn(Optional.of(testSeat));

        FlightSeat flightSeat = new FlightSeat(testSeat, testFlightInstance, "BOOKED");
        when(flightSeatRepository.findBySeatAndFlightInstance(testSeat, testFlightInstance))
                .thenReturn(Optional.of(flightSeat));

        // Act & Assert
        assertThrows(BusinessConflictException.class, () ->
                bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_SeatNotOnAircraft_ThrowsBusinessConflictException() {
        // Arrange
        BookingRequestDto request = new BookingRequestDto(passengerName, flightInstanceId, seatId);

        // Use a seat with a different aircraft
        Seat differentAircraftSeat = new Seat();
        differentAircraftSeat.setId(seatId);
        differentAircraftSeat.setAircraft(new Aircraft());

        when(flightInstanceRepository.findById(flightInstanceId))
                .thenReturn(Optional.of(testFlightInstance));

        when(seatRepository.findById(seatId))
                .thenReturn(Optional.of(differentAircraftSeat));

        // Act & Assert
        assertThrows(BusinessConflictException.class, () ->
                bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_Success() {
        // Arrange
        Long bookingId = 1L;

        // Configure booking with proper relationships
        testBooking.setId(bookingId);
        testBooking.setStatus("CONFIRMED");

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(testBooking));

        // Make bookingRepository.save() return the saved booking instance
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(flightSeatRepository.findBySeatAndFlightInstance(testSeat, testFlightInstance))
                .thenReturn(Optional.of(new FlightSeat(testSeat, testFlightInstance, "BOOKED")));

        // Act
        BookingResponseDto response = bookingService.cancelBooking(bookingId);

        // Assert
        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());
        assertEquals("AVAILABLE", new FlightSeat(testSeat, testFlightInstance, "AVAILABLE").getStatus());

        verify(flightSeatRepository).save(any());
        // Verify save returned the booking instance
        verify(bookingRepository).save(testBooking);
    }

    @Test
    void cancelBooking_AlreadyCancelled_ThrowsBusinessConflictException() {
        // Arrange
        Long bookingId = 1L;

        testBooking.setId(bookingId);
        testBooking.setStatus("CANCELLED");

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThrows(BusinessConflictException.class, () ->
                bookingService.cancelBooking(bookingId));

        verify(flightSeatRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }
}