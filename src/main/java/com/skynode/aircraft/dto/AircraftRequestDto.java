package com.skynode.aircraft.dto;

public class AircraftRequestDto {

    private String model;
    private String registrationNumber;

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

    public AircraftRequestDto() {
    }

    public AircraftRequestDto(String model, String registrationNumber) {
        this.model = model;
        this.registrationNumber = registrationNumber;
    }
}