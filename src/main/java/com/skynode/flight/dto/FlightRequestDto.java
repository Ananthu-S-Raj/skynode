package com.skynode.flight.dto;

public class FlightRequestDto {

    private Long airlineId;
    private Long originAirportId;
    private Long destinationAirportId;
    private Long aircraftId;
    private String flightNumber;

    public Long getAirlineId() {
        return airlineId;
    }

    public void setAirlineId(Long airlineId) {
        this.airlineId = airlineId;
    }

    public Long getOriginAirportId() {
        return originAirportId;
    }

    public void setOriginAirportId(Long originAirportId) {
        this.originAirportId = originAirportId;
    }

    public Long getDestinationAirportId() {
        return destinationAirportId;
    }

    public void setDestinationAirportId(Long destinationAirportId) {
        this.destinationAirportId = destinationAirportId;
    }

    public Long getAircraftId() {
        return aircraftId;
    }

    public void setAircraftId(Long aircraftId) {
        this.aircraftId = aircraftId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public FlightRequestDto() {
    }

    public FlightRequestDto(Long airlineId, Long originAirportId, Long destinationAirportId, Long aircraftId, String flightNumber) {
        this.airlineId = airlineId;
        this.originAirportId = originAirportId;
        this.destinationAirportId = destinationAirportId;
        this.aircraftId = aircraftId;
        this.flightNumber = flightNumber;
    }
}