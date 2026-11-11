package com.skynode.aircraft.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.skynode.aircraft.dto.AircraftRequestDto;
import com.skynode.aircraft.dto.AircraftResponseDto;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;
import com.skynode.ResourceNotFoundException;

@Service
public class AircraftService {

    private final AircraftRepository aircraftRepository;
    private final AirlineRepository airlineRepository;

    AircraftService(AircraftRepository aircraftRepository, AirlineRepository airlineRepository) {
        this.aircraftRepository = aircraftRepository;
        this.airlineRepository = airlineRepository;
    }

    public AircraftResponseDto createAircraft(AircraftRequestDto request, Long airlineId) {
        Airline airline = airlineRepository.findById(airlineId)
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + airlineId + " not found"));

        Aircraft aircraft = new Aircraft();
        aircraft.setAirline(airline);
        aircraft.setModel(request.getModel());
        aircraft.setRegistrationNumber(request.getRegistrationNumber());

        Aircraft savedAircraft = aircraftRepository.save(aircraft);

        return new AircraftResponseDto(
            savedAircraft.getId(),
            savedAircraft.getModel(),
            savedAircraft.getRegistrationNumber(),
            savedAircraft.getAirline().getName()
        );
    }

    public List<AircraftResponseDto> getAllAircraft() {
        List<Aircraft> aircrafts = aircraftRepository.findAll();
        List<AircraftResponseDto> response = new java.util.ArrayList<>();

        for (Aircraft aircraft : aircrafts) {
            AircraftResponseDto dto = new AircraftResponseDto(
                aircraft.getId(),
                aircraft.getModel(),
                aircraft.getRegistrationNumber(),
                aircraft.getAirline() != null ? aircraft.getAirline().getName() : null
            );
            response.add(dto);
        }

        return response;
    }

    public AircraftResponseDto getAircraftById(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft with id " + id + " not found"));

        return new AircraftResponseDto(
            aircraft.getId(),
            aircraft.getModel(),
            aircraft.getRegistrationNumber(),
            aircraft.getAirline() != null ? aircraft.getAirline().getName() : null
        );
    }

    public AircraftResponseDto updateAircraft(Long id, AircraftRequestDto request, Airline airline) {
        Aircraft aircraft = aircraftRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft with id " + id + " not found"));

        aircraft.setAirline(airline);
        aircraft.setModel(request.getModel());
        aircraft.setRegistrationNumber(request.getRegistrationNumber());

        Aircraft updatedAircraft = aircraftRepository.save(aircraft);

        return new AircraftResponseDto(
            updatedAircraft.getId(),
            updatedAircraft.getModel(),
            updatedAircraft.getRegistrationNumber(),
            updatedAircraft.getAirline().getName()
        );
    }

    public void deleteAircraft(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft with id " + id + " not found."));
        aircraftRepository.delete(aircraft);
    }
}