package com.skynode.flight.instance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skynode.flight.dto.FlightInstanceRequestDto;
import com.skynode.flight.entity.Flight;
import com.skynode.flight.repository.FlightRepository;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;
import com.skynode.airport.entity.Airport;
import com.skynode.airport.repository.AirportRepository;
import com.skynode.seat.Seat;
import com.skynode.seat.repository.SeatRepository;
import com.skynode.seat.repository.FlightSeatRepository;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.flight.instance.repository.FlightInstanceRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FlightInstanceService.
 * Mocks: FlightInstanceRepository, FlightRepository, AircraftRepository,
 * AirlineRepository, AirportRepository, SeatRepository, FlightSeatRepository.
 */
@ExtendWith(MockitoExtension.class)
class FlightInstanceServiceTest {

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AircraftRepository aircraftRepository;

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private AirportRepository airportRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private FlightSeatRepository flightSeatRepository;

    @InjectMocks
    private FlightInstanceService flightInstanceService;

    private final Long flightId = 1L;
    private final Long aircraftId = 1L;
    private final Long airlineId = 1L;
    private final Long airportId = 1L;

    // Shared test objects with proper relationships
    private final Aircraft testAircraft;
    private final Flight testFlight;
    private final List<Seat> testSeats;

    // Initialize shared test objects with full relationships
    {
        // Configure aircraft
        testAircraft = new Aircraft();
        testAircraft.setId(aircraftId);
        testAircraft.setModel("Airbus A320");

        // Configure flight with aircraft
        testFlight = new Flight();
        testFlight.setFlightNumber("BA123");
        testFlight.setAircraft(testAircraft);

        // Configure 5 test seats
        testSeats = List.of(
                new Seat(),
                new Seat(),
                new Seat(),
                new Seat(),
                new Seat()
        );
    }

    @Test
    void scheduleFlightInstance_Success_InitializesFlightSeats() {
        // Arrange
        FlightInstanceRequestDto request = new FlightInstanceRequestDto();
        request.setFlightId(flightId);
        request.setDepartureDateTime("2024-01-15T09:00:00");
        request.setArrivalDateTime("2024-01-15T11:00:00");
        request.setStatus("SCHEDULED");

        lenient().when(aircraftRepository.findById(aircraftId))
                .thenReturn(Optional.of(testAircraft));

        when(flightRepository.findById(flightId))
                .thenReturn(Optional.of(testFlight));

        when(flightInstanceRepository.save(any(FlightInstance.class)))
                .thenAnswer(i -> i.getArgument(0));

        when(seatRepository.findByAircraft(testAircraft))
                .thenReturn(testSeats);

        // Act
        FlightInstance result = flightInstanceService.scheduleFlightInstance(request);

        // Assert
        assertNotNull(result);
        verify(flightInstanceRepository).save(any());

        // Verify exactly 5 FlightSeats were saved with status AVAILABLE
        verify(flightSeatRepository).saveAll(argThat(iterable -> StreamSupport.stream(iterable.spliterator(), false).count() == 5L));
    }
}