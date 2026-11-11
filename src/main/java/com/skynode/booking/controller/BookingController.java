package com.skynode.booking.controller;

import com.skynode.booking.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.skynode.booking.dto.BookingRequestDto;
import com.skynode.booking.dto.BookingResponseDto;
import com.skynode.booking.entity.Booking;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;


@Tag(name = "booking")
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(
        summary = "Create a booking",
        description = "Create a new booking for a flight and seat",
        responses = {
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid booking details"),
            @ApiResponse(responseCode = "409", description = "Booking conflict - seat already booked or seat not on aircraft")
        }
    )
    @PostMapping
    public BookingResponseDto createBooking(@RequestBody BookingRequestDto bookingRequestDto) {
        return bookingService.createBooking(bookingRequestDto);
    }

    @Operation(
        summary = "Get a booking by ID",
        description = "Retrieve a booking by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Booking retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
        }
    )
    @GetMapping("/{id}")
    public BookingResponseDto getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @Operation(
        summary = "Get a booking by PNR",
        description = "Retrieve a booking by its PNR code",
        responses = {
            @ApiResponse(responseCode = "200", description = "Booking retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
        }
    )
    @GetMapping("/pnr/{pnr}")
    public BookingResponseDto getBookingByPnr(@PathVariable String pnr) {
        return bookingService.getBookingByPnr(pnr);
    }

    @Operation(
        summary = "Get bookings by passenger",
        description = "Retrieve all bookings for a given passenger name",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of bookings")
        }
    )
    @GetMapping("/passenger/{passengerName}")
    public List<BookingResponseDto> getBookingsByPassenger(@PathVariable String passengerName) {
        return bookingService.getBookingsByPassenger(passengerName);
    }

    @Operation(
        summary = "Cancel a booking",
        description = "Cancel an existing booking and release the seat",
        responses = {
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "409", description = "Booking already cancelled")
        }
    )
    @PutMapping("/{id}/cancel")
    public BookingResponseDto cancelBooking(@PathVariable Long id) {
        return bookingService.cancelBooking(id);
    }
}