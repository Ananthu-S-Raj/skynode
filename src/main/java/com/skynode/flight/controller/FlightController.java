package com.skynode.flight.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.skynode.flight.dto.FlightRequestDto;
import com.skynode.flight.dto.FlightResponseDto;
import com.skynode.flight.service.FlightService;

@Tag(name = "flight")
@RestController
@RequestMapping("/api/v1/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @Operation(summary = "Create a flight")
    @PostMapping
    public FlightResponseDto createFlight(@Valid @RequestBody FlightRequestDto flightRequestDto) {
        return flightService.createFlight(flightRequestDto);
    }

    @Operation(summary = "Get all flights")
    @GetMapping
    public List<FlightResponseDto> getAllFlights() {
        return flightService.getAllFlights();
    }

    @Operation(summary = "Get a flight by ID")
    @GetMapping("/{id}")
    public FlightResponseDto getFlightById(@PathVariable Long id) {
        return flightService.getFlightById(id);
    }

    @Operation(summary = "Update a flight")
    @PutMapping("/{id}")
    public FlightResponseDto updateFlight(@PathVariable Long id, @Valid @RequestBody FlightRequestDto flightRequestDto) {
        return flightService.updateFlight(id, flightRequestDto);
    }

    @Operation(summary = "Delete a flight")
    @DeleteMapping("/{id}")
    public void deleteFlight(@PathVariable Long id) {
        flightService.deleteFlight(id);
    }
}
