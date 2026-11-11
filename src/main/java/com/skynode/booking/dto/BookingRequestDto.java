package com.skynode.booking.dto;

public class BookingRequestDto {

    private String passengerName;
    private Long flightInstanceId;
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