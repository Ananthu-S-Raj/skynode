package com.skynode.aircraft.dto;

public class AircraftResponseDto {

    private Long id;
    private String model;
    private String registrationNumber;
    private String airlineName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }

    public AircraftResponseDto() {
    }

    public AircraftResponseDto(Long id, String model, String registrationNumber, String airlineName) {
        this.id = id;
        this.model = model;
        this.registrationNumber = registrationNumber;
        this.airlineName = airlineName;
    }
}
