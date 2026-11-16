package com.skynode.flight.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skynode.BusinessConflictException;
import com.skynode.ResourceNotFoundException;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;
import com.skynode.airport.entity.Airport;
import com.skynode.airport.repository.AirportRepository;
import com.skynode.flight.dto.FlightRequestDto;
import com.skynode.flight.dto.FlightResponseDto;
import com.skynode.flight.entity.Flight;
import com.skynode.flight.instance.repository.FlightInstanceRepository;
import com.skynode.flight.repository.FlightRepository;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final AircraftRepository aircraftRepository;
    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;

    public FlightService(FlightRepository flightRepository,
                         AircraftRepository aircraftRepository,
                         AirlineRepository airlineRepository,
                         AirportRepository airportRepository,
                         FlightInstanceRepository flightInstanceRepository) {
        this.flightRepository = flightRepository;
        this.flightInstanceRepository = flightInstanceRepository;
        this.aircraftRepository = aircraftRepository;
        this.airlineRepository = airlineRepository;
        this.airportRepository = airportRepository;
    }

    public FlightResponseDto createFlight(FlightRequestDto request) {
        Flight flight = new Flight();
        applyRequest(flight, request);

        Flight savedFlight = flightRepository.save(flight);

        return toResponse(savedFlight);
    }

    public List<FlightResponseDto> getAllFlights() {
        List<Flight> flights = flightRepository.findAll();
        List<FlightResponseDto> response = new ArrayList<>();

        for (Flight flight : flights) {
            response.add(toResponse(flight));
        }

        return response;
    }

    public FlightResponseDto getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Flight with id " + id + " not found"));

        return toResponse(flight);
    }

    @Transactional
    public FlightResponseDto updateFlight(Long id, FlightRequestDto request) {
        Flight flight = flightRepository.findForUpdateById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Flight with id " + id + " not found"));

        if (!flight.getAircraft().getId().equals(request.getAircraftId())
                && flightInstanceRepository.existsByFlightId(id)) {
            throw new BusinessConflictException("Cannot change aircraft after flight instances have been scheduled");
        }
        applyRequest(flight, request);

        Flight updatedFlight = flightRepository.save(flight);

        return toResponse(updatedFlight);
    }

    public void deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Flight with id " + id + " not found."));
        flightRepository.delete(flight);
    }

    private void applyRequest(Flight flight, FlightRequestDto request) {
        Airport origin = airportRepository.findById(request.getOriginAirportId())
            .orElseThrow(() -> new ResourceNotFoundException("Origin airport with id " + request.getOriginAirportId() + " not found"));

        Airport destination = airportRepository.findById(request.getDestinationAirportId())
            .orElseThrow(() -> new ResourceNotFoundException("Destination airport with id " + request.getDestinationAirportId() + " not found"));

        if (origin.getId().equals(destination.getId())) {
            throw new BusinessConflictException("Origin and destination cannot be the same airport");
        }

        Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft with id " + request.getAircraftId() + " not found"));

        Airline airline = airlineRepository.findById(request.getAirlineId())
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + request.getAirlineId() + " not found"));

        flight.setAirline(airline);
        flight.setOrigin(origin);
        flight.setDestination(destination);
        flight.setAircraft(aircraft);
        flight.setFlightNumber(request.getFlightNumber());
    }

    private FlightResponseDto toResponse(Flight flight) {
        return new FlightResponseDto(
            flight.getId(),
            flight.getFlightNumber(),
            flight.getAirline().getName(),
            flight.getOrigin().getName(),
            flight.getDestination().getName(),
            flight.getAircraft().getModel(),
            flight.getAircraft().getRegistrationNumber()
        );
    }
}
