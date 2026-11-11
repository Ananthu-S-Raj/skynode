package com.skynode.flight.dto;

public class FlightInstanceRequestDto {

    private Long flightId;
    private String departureDateTime;
    private String arrivalDateTime;
    private String status;

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
    }

    public String getDepartureDateTime() {
        return departureDateTime;
    }

    public void setDepartureDateTime(String departureDateTime) {
        this.departureDateTime = departureDateTime;
    }

    public String getArrivalDateTime() {
        return arrivalDateTime;
    }

    public void setArrivalDateTime(String arrivalDateTime) {
        this.arrivalDateTime = arrivalDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public FlightInstanceRequestDto() {
    }

    public FlightInstanceRequestDto(Long flightId, String departureDateTime, String arrivalDateTime, String status) {
        this.flightId = flightId;
        this.departureDateTime = departureDateTime;
        this.arrivalDateTime = arrivalDateTime;
        this.status = status;
    }
}