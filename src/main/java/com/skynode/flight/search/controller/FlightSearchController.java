package com.skynode.flight.search.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.skynode.flight.search.dto.FlightSearchRequestDto;
import com.skynode.flight.search.dto.FlightSearchResultDto;
import com.skynode.flight.search.service.FlightSearchService;

@Tag(name = "flight-search")
@RestController
@RequestMapping("/api/v1/flights/search")
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    public FlightSearchController(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    @Operation(summary = "Search flights")
    @GetMapping
    public List<FlightSearchResultDto> searchFlights(FlightSearchRequestDto request) {
        return flightSearchService.search(request);
    }
}
