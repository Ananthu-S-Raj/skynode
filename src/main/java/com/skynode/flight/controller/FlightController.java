package com.skynode.flight.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.skynode.flight.dto.FlightRequestDto;
import com.skynode.flight.dto.FlightResponseDto;
import com.skynode.flight.entity.Flight;
import com.skynode.flight.repository.FlightRepository;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.airline.entity.Airline;
import com.skynode.airline.repository.AirlineRepository;
import com.skynode.airport.entity.Airport;
import com.skynode.airport.repository.AirportRepository;
import com.skynode.flight.service.FlightService;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;


@Tag(name = "flight")
@RestController
@RequestMapping("/api/v1/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @Operation(
        summary = "Create a flight",
        description = "Create a new flight with the given details",
        responses = {
            @ApiResponse(responseCode = "201", description = "Flight created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid flight details"),
            @ApiResponse(responseCode = "409", description = "Flight already exists")
        }
    )
    @PostMapping
    public FlightResponseDto createFlight(@RequestBody FlightRequestDto flightRequestDto) {
        return flightService.createFlight(flightRequestDto);
    }

    @Operation(
        summary = "Get all flights",
        description = "Retrieve a list of all flights",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of flights")
        }
    )
    @GetMapping
    public List<FlightResponseDto> getAllFlights() {
        return flightService.getAllFlights();
    }

    @Operation(
        summary = "Get a flight by ID",
        description = "Retrieve a flight by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Flight retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
        }
    )
    @GetMapping("/{id}")
    public FlightResponseDto getFlightById(@PathVariable Long id) {
        return flightService.getFlightById(id);
    }

    @Operation(
        summary = "Update a flight",
        description = "Update an existing flight with the given details",
        responses = {
            @ApiResponse(responseCode = "200", description = "Flight updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid flight details"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
        }
    )
    @PutMapping("/{id}")
    public FlightResponseDto updateFlight(@PathVariable Long id, @RequestBody FlightRequestDto flightRequestDto) {
        return flightService.updateFlight(id, flightRequestDto);
    }

    @Operation(
        summary = "Delete a flight",
        description = "Delete a flight by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "204", description = "Flight deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
        }
    )
    @DeleteMapping("/{id}")
    public void deleteFlight(@PathVariable Long id) {
        flightService.deleteFlight(id);
    }
}