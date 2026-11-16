package com.skynode.airline.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.skynode.ResourceNotFoundException;
import com.skynode.airline.dto.AirlineRequestDto;
import com.skynode.airline.dto.AirlineResponseDto;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;

@Service
public class AirlineService {

    private final AirlineRepository airlineRepository;

    public AirlineService(AirlineRepository airlineRepository) {
        this.airlineRepository = airlineRepository;
    }

    public AirlineResponseDto createAirline(AirlineRequestDto request) {
        Airline airline = new Airline();
        airline.setName(request.getName());
        airline.setCode(request.getCode());

        Airline savedAirline = airlineRepository.save(airline);

        return toResponse(savedAirline);
    }

    public List<AirlineResponseDto> getAllAirlines() {
        List<Airline> airlines = airlineRepository.findAll();
        List<AirlineResponseDto> response = new ArrayList<>();

        for (Airline airline : airlines) {
            response.add(toResponse(airline));
        }

        return response;
    }

    public AirlineResponseDto getAirlineById(Long id) {
        Airline airline = airlineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + id + " not found"));

        return toResponse(airline);
    }

    public AirlineResponseDto updateAirline(Long id, AirlineRequestDto request) {
        Airline airline = airlineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + id + " not found"));

        airline.setName(request.getName());
        airline.setCode(request.getCode());

        Airline updatedAirline = airlineRepository.save(airline);

        return toResponse(updatedAirline);
    }

    public void deleteAirline(Long id) {
        Airline airline = airlineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + id + " not found."));
        airlineRepository.delete(airline);
    }

    private AirlineResponseDto toResponse(Airline airline) {
        return new AirlineResponseDto(
            airline.getId(),
            airline.getName(),
            airline.getCode()
        );
    }
}
