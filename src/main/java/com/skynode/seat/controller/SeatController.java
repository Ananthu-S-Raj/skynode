package com.skynode.seat.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skynode.seat.dto.SeatRequestDto;
import com.skynode.seat.dto.SeatResponseDto;
import com.skynode.seat.service.SeatService;

@RestController
@RequestMapping("/api/v1/aircraft/{aircraftId}/seats")
public class SeatController {
    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping
    public SeatResponseDto addSeat(@PathVariable Long aircraftId, @Valid @RequestBody SeatRequestDto request) {
        return seatService.addSeat(aircraftId, request);
    }

    @GetMapping
    public List<SeatResponseDto> getSeats(@PathVariable Long aircraftId) {
        return seatService.getSeats(aircraftId);
    }

    @GetMapping("/{seatNumber}")
    public SeatResponseDto getSeat(@PathVariable Long aircraftId, @PathVariable String seatNumber) {
        return seatService.getSeat(aircraftId, seatNumber);
    }
}
