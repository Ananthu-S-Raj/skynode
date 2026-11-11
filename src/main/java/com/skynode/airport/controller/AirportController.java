package com.skynode.airport.controller;

import com.skynode.airport.dto.AirportRequestDto;
import com.skynode.airport.dto.AirportResponseDto;
import com.skynode.airport.entity.Airport;
import com.skynode.airport.repository.AirportRepository;
import com.skynode.airport.service.AirportService;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/airports")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @PostMapping
    public AirportResponseDto addAirport(@RequestBody AirportRequestDto airportRequestDto) {
        return airportService.addAirport(airportRequestDto);
    }

    @GetMapping
    public List<AirportResponseDto> getAllAirports() {
        return airportService.getAllAirports();
    }

    @GetMapping("/{id}")
    public AirportResponseDto getAirportById(@PathVariable Long id) {
        return airportService.getAirportById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteAirportById(@PathVariable Long id) {
        airportService.deleteAirportById(id);
    }
}