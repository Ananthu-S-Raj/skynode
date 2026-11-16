package com.skynode.flight.instance.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skynode.InvalidRequestException;
import com.skynode.flight.dto.FlightInstanceRequestDto;
import com.skynode.flight.dto.FlightInstanceResponseDto;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.flight.instance.service.FlightInstanceService;

@RestController
@RequestMapping("/api/v1/flight-instances")
public class FlightInstanceController {
    private final FlightInstanceService flightInstanceService;

    public FlightInstanceController(FlightInstanceService flightInstanceService) {
        this.flightInstanceService = flightInstanceService;
    }

    @PostMapping
    public FlightInstanceResponseDto schedule(@RequestBody FlightInstanceRequestDto request,
                                             @RequestParam(required = false) Long flightId) {
        if (flightId != null) {
            if (request.getFlightId() != null && !flightId.equals(request.getFlightId())) {
                throw new InvalidRequestException("Body and query flightId must match");
            }
            request.setFlightId(flightId);
        }
        return toResponse(flightInstanceService.scheduleFlightInstance(request));
    }

    @GetMapping
    public List<FlightInstanceResponseDto> getAll() {
        return flightInstanceService.getAllInstances().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public FlightInstanceResponseDto getById(@PathVariable Long id) {
        return toResponse(flightInstanceService.getInstanceById(id));
    }

    @GetMapping("/flight/{flightId}")
    public List<FlightInstanceResponseDto> getByFlight(@PathVariable Long flightId) {
        return flightInstanceService.getInstancesByFlightId(flightId).stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}")
    public FlightInstanceResponseDto update(@PathVariable Long id, @RequestBody FlightInstanceRequestDto request) {
        return toResponse(flightInstanceService.updateFlightInstance(id, request));
    }

    @PutMapping("/{id}/status")
    public FlightInstanceResponseDto updateStatus(@PathVariable Long id, @RequestParam String status) {
        flightInstanceService.updateFlightStatus(id, status);
        return toResponse(flightInstanceService.getInstanceById(id));
    }

    private FlightInstanceResponseDto toResponse(FlightInstance instance) {
        return new FlightInstanceResponseDto(instance.getId(), instance.getFlight().getId(),
            instance.getDepartureDateTime(), instance.getArrivalDateTime(), instance.getStatus());
    }
}
