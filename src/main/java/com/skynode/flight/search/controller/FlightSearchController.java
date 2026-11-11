package com.skynode.flight.search.controller;

import com.skynode.flight.search.service.FlightSearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.skynode.flight.search.dto.FlightSearchRequestDto;
import com.skynode.flight.search.dto.FlightSearchResultDto;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;


@Tag(name = "flight-search")
@RestController
@RequestMapping("/api/v1/flights/search")
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    public FlightSearchController(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    @Operation(
        summary = "Search flights",
        description = "Search flights by origin, destination, and departure date",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful search result"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    @GetMapping
    public List<FlightSearchResultDto> searchFlights(FlightSearchRequestDto request) {
        return flightSearchService.search(request);
    }
}