package com.skynode.airport.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skynode.airport.dto.AirportRequestDto;
import com.skynode.airport.dto.AirportResponseDto;
import com.skynode.airport.service.AirportService;

@RestController
@RequestMapping("/api/v1/airports")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @PostMapping
    public AirportResponseDto addAirport(@Valid @RequestBody AirportRequestDto airportRequestDto) {
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
