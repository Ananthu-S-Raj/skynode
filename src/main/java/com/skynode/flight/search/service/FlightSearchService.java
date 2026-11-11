package com.skynode.flight.search.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.skynode.flight.entity.Flight;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.flight.instance.repository.FlightInstanceRepository;
import com.skynode.flight.search.dto.FlightSearchResultDto;
import com.skynode.flight.search.dto.FlightSearchRequestDto;
import com.skynode.seat.repository.FlightSeatRepository;

@Service
public class FlightSearchService {

    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightSeatRepository flightSeatRepository;

    public FlightSearchService(FlightInstanceRepository flightInstanceRepository,
                               FlightSeatRepository flightSeatRepository) {
        this.flightInstanceRepository = flightInstanceRepository;
        this.flightSeatRepository = flightSeatRepository;
    }

    public List<FlightSearchResultDto> search(FlightSearchRequestDto request) {
        String datePattern = "%" + request.getDepartureDate() + "%";

        List<FlightInstance> instances = flightInstanceRepository.findByDepartureDate(datePattern);

        return instances.stream()
            .filter(instance -> matchesOriginAndDestination(instance, request))
            .map(instance -> {
                Flight flight = instance.getFlight();
                long availableSeatCount = countAvailableSeats(instance);

                return new FlightSearchResultDto(
                    flight.getFlightNumber(),
                    flight.getAirline().getName(),
                    flight.getOrigin().getName(),
                    flight.getDestination().getName(),
                    instance.getDepartureDateTime(),
                    instance.getArrivalDateTime(),
                    instance.getStatus(),
                    availableSeatCount
                );
            })
            .collect(Collectors.toList());
    }

    private boolean matchesOriginAndDestination(FlightInstance instance, FlightSearchRequestDto request) {
        Long originId = request.getOriginAirportId();
        Long destinationId = request.getDestinationAirportId();

        if (originId == null || destinationId == null) {
            return true;
        }

        Long instanceOriginId = instance.getFlight().getOrigin().getId();
        Long instanceDestinationId = instance.getFlight().getDestination().getId();

        return originId.equals(instanceOriginId) && destinationId.equals(instanceDestinationId);
    }

    private long countAvailableSeats(FlightInstance flightInstance) {
        // Count AVAILABLE FlightSeat entities for this flight instance
        return flightSeatRepository.countByFlightInstanceAndStatus(flightInstance, "AVAILABLE");
    }
}