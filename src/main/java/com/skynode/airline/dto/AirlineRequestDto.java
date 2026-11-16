package com.skynode.airline.dto;

import jakarta.validation.constraints.NotBlank;

public class AirlineRequestDto {

    @NotBlank
    private String name;
    @NotBlank
    private String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public AirlineRequestDto() {
    }

    public AirlineRequestDto(String name, String code) {
        this.name = name;
        this.code = code;
    }
}
