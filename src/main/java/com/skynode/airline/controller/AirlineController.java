package com.skynode.airline.controller;

import com.skynode.airline.dto.AirlineRequestDto;
import com.skynode.airline.dto.AirlineResponseDto;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;
import com.skynode.airline.service.AirlineService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/airlines")
public class AirlineController {

    private final AirlineService airlineService;

    public AirlineController(AirlineService airlineService) {
        this.airlineService = airlineService;
    }

    @PostMapping
    public ResponseEntity<AirlineResponseDto> createAirline(@RequestBody AirlineRequestDto airlineRequestDto) {
        AirlineResponseDto created = airlineService.createAirline(airlineRequestDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public List<AirlineResponseDto> getAllAirlines() {
        return airlineService.getAllAirlines();
    }

    @GetMapping("/{id}")
    public AirlineResponseDto getAirlineById(@PathVariable Long id) {
        return airlineService.getAirlineById(id);
    }

    @PutMapping("/{id}")
    public AirlineResponseDto updateAirline(@PathVariable Long id, @RequestBody AirlineRequestDto airlineRequestDto) {
        return airlineService.updateAirline(id, airlineRequestDto);
    }

    @DeleteMapping("/{id}")
    public void deleteAirline(@PathVariable Long id) {
        airlineService.deleteAirline(id);
    }
}