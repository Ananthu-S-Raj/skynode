package com.skynode.aircraft.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skynode.aircraft.dto.AircraftRequestDto;
import com.skynode.aircraft.dto.AircraftResponseDto;
import com.skynode.aircraft.service.AircraftService;

@RestController
@RequestMapping("/api/v1/aircraft")
public class AircraftController {

    private final AircraftService aircraftService;

    public AircraftController(AircraftService aircraftService) {
        this.aircraftService = aircraftService;
    }

    @PostMapping
    public AircraftResponseDto createAircraft(@Valid @RequestBody AircraftRequestDto aircraftRequestDto,
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
    public AircraftResponseDto updateAircraft(@PathVariable Long id, @Valid @RequestBody AircraftRequestDto aircraftRequestDto,
                                              @RequestParam Long airlineId) {
        return aircraftService.updateAircraft(id, aircraftRequestDto, airlineId);
    }

    @DeleteMapping("/{id}")
    public void deleteAircraft(@PathVariable Long id) {
        aircraftService.deleteAircraft(id);
    }
}
