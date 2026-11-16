package com.skynode.flight.search.dto;

public class FlightSearchResultDto {

    private String flightNumber;
    private String airlineName;
    private String originName;
    private String destinationName;
    private String departureTime;
    private String arrivalTime;
    private String status;
    private long availableSeatCount;

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

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getAvailableSeatCount() {
        return availableSeatCount;
    }

    public void setAvailableSeatCount(long availableSeatCount) {
        this.availableSeatCount = availableSeatCount;
    }

    public FlightSearchResultDto() {
    }

    public FlightSearchResultDto(String flightNumber, String airlineName, String originName,
                                 String destinationName, String departureTime, String arrivalTime,
                                 String status, long availableSeatCount) {
        this.flightNumber = flightNumber;
        this.airlineName = airlineName;
        this.originName = originName;
        this.destinationName = destinationName;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.status = status;
        this.availableSeatCount = availableSeatCount;
    }
}
