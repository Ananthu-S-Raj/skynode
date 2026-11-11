package com.skynode.airline.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.skynode.airline.dto.AirlineRequestDto;
import com.skynode.airline.dto.AirlineResponseDto;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;
import com.skynode.ResourceNotFoundException;

@Service
public class AirlineService {

    private final AirlineRepository airlineRepository;

    AirlineService(AirlineRepository airlineRepository) {
        this.airlineRepository = airlineRepository;
    }

    public AirlineResponseDto createAirline(AirlineRequestDto request) {
        Airline airline = new Airline();
        airline.setName(request.getName());
        airline.setCode(request.getCode());

        Airline savedAirline = airlineRepository.save(airline);

        return new AirlineResponseDto(
            savedAirline.getId(),
            savedAirline.getName(),
            savedAirline.getCode()
        );
    }

    public List<AirlineResponseDto> getAllAirlines() {
        List<Airline> airlines = airlineRepository.findAll();
        List<AirlineResponseDto> response = new java.util.ArrayList<>();

        for (Airline airline : airlines) {
            AirlineResponseDto dto = new AirlineResponseDto(
                airline.getId(),
                airline.getName(),
                airline.getCode()
            );
            response.add(dto);
        }

        return response;
    }

    public AirlineResponseDto getAirlineById(Long id) {
        Airline airline = airlineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + id + " not found"));

        return new AirlineResponseDto(
            airline.getId(),
            airline.getName(),
            airline.getCode()
        );
    }

    public AirlineResponseDto updateAirline(Long id, AirlineRequestDto request) {
        Airline airline = airlineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + id + " not found"));

        airline.setName(request.getName());
        airline.setCode(request.getCode());

        Airline updatedAirline = airlineRepository.save(airline);

        return new AirlineResponseDto(
            updatedAirline.getId(),
            updatedAirline.getName(),
            updatedAirline.getCode()
        );
    }

    public void deleteAirline(Long id) {
        Airline airline = airlineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + id + " not found."));
        airlineRepository.delete(airline);
    }
}