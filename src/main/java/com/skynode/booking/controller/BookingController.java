package com.skynode.booking.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.skynode.booking.dto.BookingRequestDto;
import com.skynode.booking.dto.BookingResponseDto;
import com.skynode.booking.service.BookingService;

@Tag(name = "booking")
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Create a booking")
    @PostMapping
    public BookingResponseDto createBooking(@Valid @RequestBody BookingRequestDto bookingRequestDto) {
        return bookingService.createBooking(bookingRequestDto);
    }

    @Operation(summary = "Get a booking by ID")
    @GetMapping("/{id}")
    public BookingResponseDto getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @Operation(summary = "Get a booking by PNR")
    @GetMapping("/pnr/{pnr}")
    public BookingResponseDto getBookingByPnr(@PathVariable String pnr) {
        return bookingService.getBookingByPnr(pnr);
    }

    @Operation(summary = "Get bookings by passenger")
    @GetMapping("/passenger/{passengerName}")
    public List<BookingResponseDto> getBookingsByPassenger(@PathVariable String passengerName) {
        return bookingService.getBookingsByPassenger(passengerName);
    }

    @Operation(summary = "Cancel a booking")
    @PutMapping("/{id}/cancel")
    public BookingResponseDto cancelBooking(@PathVariable Long id) {
        return bookingService.cancelBooking(id);
    }
}
