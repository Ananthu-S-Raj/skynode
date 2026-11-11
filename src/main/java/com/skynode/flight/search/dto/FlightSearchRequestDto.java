package com.skynode.flight.search.dto;

public class FlightSearchRequestDto {

    private Long originAirportId;
    private Long destinationAirportId;
    private String departureDate;

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

    public String getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }

    public FlightSearchRequestDto() {
    }

    public FlightSearchRequestDto(Long originAirportId, Long destinationAirportId, String departureDate) {
        this.originAirportId = originAirportId;
        this.destinationAirportId = destinationAirportId;
        this.departureDate = departureDate;
    }
}