package com.skynode.flight.dto;

public record FlightInstanceResponseDto(Long id, Long flightId, String departureDateTime,
                                        String arrivalDateTime, String status) {
}
