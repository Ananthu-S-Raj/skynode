package com.skynode.flight.dto;

public class FlightResponseDto {

    private Long id;
    private String flightNumber;
    private String airlineName;
    private String originName;
    private String destinationName;
    private String aircraftModel;
    private String aircraftRegistration;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getAircraftModel() {
        return aircraftModel;
    }

    public void setAircraftModel(String aircraftModel) {
        this.aircraftModel = aircraftModel;
    }

    public String getAircraftRegistration() {
        return aircraftRegistration;
    }

    public void setAircraftRegistration(String aircraftRegistration) {
        this.aircraftRegistration = aircraftRegistration;
    }

    public FlightResponseDto() {
    }

    public FlightResponseDto(Long id, String flightNumber, String airlineName, String originName, String destinationName, String aircraftModel, String aircraftRegistration) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.airlineName = airlineName;
        this.originName = originName;
        this.destinationName = destinationName;
        this.aircraftModel = aircraftModel;
        this.aircraftRegistration = aircraftRegistration;
    }
}
