package com.skynode.flight.search.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skynode.flight.search.dto.FlightSearchRequestDto;
import com.skynode.flight.search.dto.FlightSearchResultDto;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.flight.instance.repository.FlightInstanceRepository;
import com.skynode.seat.repository.FlightSeatRepository;
import com.skynode.flight.entity.Flight;
import com.skynode.airport.entity.Airport;
import com.skynode.airline.entity.Airline;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FlightSearchService.
 * Mocks: FlightInstanceRepository, FlightSeatRepository.
 */
@ExtendWith(MockitoExtension.class)
class FlightSearchServiceTest {

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private FlightSeatRepository flightSeatRepository;

    @InjectMocks
    private FlightSearchService flightSearchService;

    @Test
    void search_Flights_ReturnsResults_WithAvailableSeatCount() {
        // Arrange
        FlightSearchRequestDto request = new FlightSearchRequestDto();
        request.setOriginAirportId(1L);
        request.setDestinationAirportId(2L);
        request.setDepartureDate("2024-01-15");

        // Create FlightInstance with populated Flight relationship
        FlightInstance instance = new FlightInstance();
        Flight flight = new Flight();
        flight.setFlightNumber("BA123");

        Airline airline = new Airline("BA", "British Airways");
        flight.setAirline(airline);

        Airport originAirport = new Airport();
        originAirport.setId(1L);
        originAirport.setName("Origin Airport");

        Airport destinationAirport = new Airport();
        destinationAirport.setId(2L);
        destinationAirport.setName("Destination Airport");

        flight.setOrigin(originAirport);
        flight.setDestination(destinationAirport);
        instance.setFlight(flight);

        when(flightInstanceRepository.findByDepartureDate(anyString()))
                .thenReturn(List.of(instance));

when(flightSeatRepository.countByFlightInstanceAndStatus(instance, "AVAILABLE"))
                .thenReturn(3L);

        // Act
        List<FlightSearchResultDto> results = flightSearchService.search(request);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        FlightSearchResultDto result = results.get(0);
        assertNotNull(result);
        assertEquals(3L, result.getAvailableSeatCount());

        // Verify the repository was called with correct arguments
        verify(flightInstanceRepository).findByDepartureDate(anyString());
        verify(flightSeatRepository).countByFlightInstanceAndStatus(instance, "AVAILABLE");
    }
}