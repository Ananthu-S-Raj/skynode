package com.skynode.airport.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.skynode.ResourceNotFoundException;
import com.skynode.airport.dto.AirportRequestDto;
import com.skynode.airport.dto.AirportResponseDto;
import com.skynode.airport.entity.Airport;
import com.skynode.airport.repository.AirportRepository;

@Service
public class AirportService {

    private final AirportRepository airportRepository;

    public AirportService(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    public AirportResponseDto addAirport(AirportRequestDto request) {
        Airport airport = new Airport();
        airport.setName(request.getName());
        airport.setCountry(request.getCountry());
        airport.setCity(request.getCity());

        Airport savedAirport = airportRepository.save(airport);

        return toResponse(savedAirport);
    }

    public List<AirportResponseDto> getAllAirports() {
        List<Airport> airports = airportRepository.findAll();
        List<AirportResponseDto> response = new ArrayList<>();

        for (Airport airport : airports) {
            response.add(toResponse(airport));
        }

        return response;
    }

    public AirportResponseDto getAirportById(Long id) {
        Airport airport = airportRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Airport with id " + id + " not found"));

        return toResponse(airport);
    }

    public void deleteAirportById(Long id) {
        Airport airport = airportRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Airport with id " + id + " not found."));
        airportRepository.delete(airport);
    }

    private AirportResponseDto toResponse(Airport airport) {
        return new AirportResponseDto(
            airport.getId(),
            airport.getName(),
            airport.getCountry(),
            airport.getCity()
        );
    }
}
