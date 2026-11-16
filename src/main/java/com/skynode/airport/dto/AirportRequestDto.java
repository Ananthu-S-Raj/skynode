package com.skynode.airport.dto;

import jakarta.validation.constraints.NotBlank;

public class AirportRequestDto {

    @NotBlank
    private String name;
    @NotBlank
    private String country;
    @NotBlank
    private String city;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public AirportRequestDto(String name, String country, String city) {
        this.name = name;
        this.country = country;
        this.city = city;
    }

    public AirportRequestDto() {
    }
}
