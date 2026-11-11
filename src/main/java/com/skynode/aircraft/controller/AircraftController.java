package com.skynode.aircraft.controller;

import com.skynode.aircraft.dto.AircraftRequestDto;
import com.skynode.aircraft.dto.AircraftResponseDto;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.aircraft.service.AircraftService;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;
import com.skynode.ResourceNotFoundException;

import java.util.List;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/aircraft")
public class AircraftController {

    private final AircraftService aircraftService;
    private final AirlineRepository airlineRepository;

    public AircraftController(AircraftService aircraftService, AirlineRepository airlineRepository) {
        this.aircraftService = aircraftService;
        this.airlineRepository = airlineRepository;
    }

    @PostMapping
    public AircraftResponseDto createAircraft(@RequestBody AircraftRequestDto aircraftRequestDto,
                                              @RequestParam Long airlineId) {
        return aircraftService.createAircraft(aircraftRequestDto, airlineId);
    }

    @GetMapping
    public List<AircraftResponseDto> getAllAircraft() {
        return aircraftService.getAllAircraft();
    }

    @GetMapping("/{id}")
    public AircraftResponseDto getAircraftById(@PathVariable Long id) {
        return aircraftService.getAircraftById(id);
    }

    @PutMapping("/{id}")
    public AircraftResponseDto updateAircraft(@PathVariable Long id, @RequestBody AircraftRequestDto aircraftRequestDto,
                                              @RequestParam Long airlineId) {
        Airline airline = airlineRepository.findById(airlineId)
            .orElseThrow(() -> new ResourceNotFoundException("Airline with id " + airlineId + " not found"));
        return aircraftService.updateAircraft(id, aircraftRequestDto, airline);
    }

    @DeleteMapping("/{id}")
    public void deleteAircraft(@PathVariable Long id) {
        aircraftService.deleteAircraft(id);
    }
}