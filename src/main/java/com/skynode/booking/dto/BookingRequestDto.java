package com.skynode.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class BookingRequestDto {

    @NotBlank
    private String passengerName;
    @NotNull
    @Positive
    private Long flightInstanceId;
    @NotNull
    @Positive
    private Long seatId;

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public Long getFlightInstanceId() {
        return flightInstanceId;
    }

    public void setFlightInstanceId(Long flightInstanceId) {
        this.flightInstanceId = flightInstanceId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public BookingRequestDto() {
    }

    public BookingRequestDto(String passengerName, Long flightInstanceId, Long seatId) {
        this.passengerName = passengerName;
        this.flightInstanceId = flightInstanceId;
        this.seatId = seatId;
    }
}
