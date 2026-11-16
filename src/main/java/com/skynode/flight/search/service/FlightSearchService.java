package com.skynode.flight.search.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.skynode.flight.entity.Flight;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.flight.instance.repository.FlightInstanceRepository;
import com.skynode.flight.search.dto.FlightSearchRequestDto;
import com.skynode.flight.search.dto.FlightSearchResultDto;
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
        String datePattern = request.getDepartureDate() == null || request.getDepartureDate().isBlank()
            ? "%" : request.getDepartureDate() + "%";

        List<FlightInstance> instances = flightInstanceRepository.findByDepartureDate(datePattern);

        List<FlightSearchResultDto> results = new ArrayList<>();
        for (FlightInstance instance : instances) {
            Flight flight = instance.getFlight();
            if (request.getOriginAirportId() != null
                    && !request.getOriginAirportId().equals(flight.getOrigin().getId())) {
                continue;
            }
            if (request.getDestinationAirportId() != null
                    && !request.getDestinationAirportId().equals(flight.getDestination().getId())) {
                continue;
            }

            long availableSeats = flightSeatRepository.countByFlightInstanceAndStatus(instance, "AVAILABLE");
            results.add(new FlightSearchResultDto(
                flight.getFlightNumber(),
                flight.getAirline().getName(),
                flight.getOrigin().getName(),
                flight.getDestination().getName(),
                instance.getDepartureDateTime(),
                instance.getArrivalDateTime(),
                instance.getStatus(),
                availableSeats
            ));
        }
        return results;
    }
}
