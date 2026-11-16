package com.skynode.aircraft.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.skynode.ResourceNotFoundException;
import com.skynode.aircraft.dto.AircraftRequestDto;
import com.skynode.aircraft.dto.AircraftResponseDto;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;

@Service
public class AircraftService {

    private final AircraftRepository aircraftRepository;
    private final AirlineRepository airlineRepository;

    public AircraftService(AircraftRepository aircraftRepository, AirlineRepository airlineRepository) {
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

        return toResponse(savedAircraft);
    }

    public List<AircraftResponseDto> getAllAircraft() {
        List<Aircraft> aircrafts = aircraftRepository.findAll();
        List<AircraftResponseDto> response = new ArrayList<>();

        for (Aircraft aircraft : aircrafts) {
            response.add(toResponse(aircraft));
        }

        return response;
    }

    public AircraftResponseDto getAircraftById(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft with id " + id + " not found"));

        return toResponse(aircraft);
    }

    public AircraftResponseDto updateAircraft(Long id, AircraftRequestDto request, Long airlineId) {
        Aircraft aircraft = aircraftRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft with id " + id + " not found"));

        Airline airline = airlineRepository.findById(airlineId)
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + airlineId + " not found"));

        aircraft.setAirline(airline);
        aircraft.setModel(request.getModel());
        aircraft.setRegistrationNumber(request.getRegistrationNumber());

        Aircraft updatedAircraft = aircraftRepository.save(aircraft);

        return toResponse(updatedAircraft);
    }

    public void deleteAircraft(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft with id " + id + " not found."));
        aircraftRepository.delete(aircraft);
    }

    private AircraftResponseDto toResponse(Aircraft aircraft) {
        return new AircraftResponseDto(
            aircraft.getId(),
            aircraft.getModel(),
            aircraft.getRegistrationNumber(),
            aircraft.getAirline().getName()
        );
    }
}
