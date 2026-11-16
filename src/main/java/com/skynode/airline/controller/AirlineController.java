package com.skynode.airline.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skynode.airline.dto.AirlineRequestDto;
import com.skynode.airline.dto.AirlineResponseDto;
import com.skynode.airline.service.AirlineService;

@RestController
@RequestMapping("/api/v1/airlines")
public class AirlineController {

    private final AirlineService airlineService;

    public AirlineController(AirlineService airlineService) {
        this.airlineService = airlineService;
    }

    @PostMapping
    public ResponseEntity<AirlineResponseDto> createAirline(@Valid @RequestBody AirlineRequestDto airlineRequestDto) {
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
    public AirlineResponseDto updateAirline(@PathVariable Long id, @Valid @RequestBody AirlineRequestDto airlineRequestDto) {
        return airlineService.updateAirline(id, airlineRequestDto);
    }

    @DeleteMapping("/{id}")
    public void deleteAirline(@PathVariable Long id) {
        airlineService.deleteAirline(id);
    }
}
