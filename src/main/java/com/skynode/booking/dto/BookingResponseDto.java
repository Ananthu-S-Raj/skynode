package com.skynode.booking.dto;

public class BookingResponseDto {

    private Long id;
    private String pnr;
    private String passengerName;
    private String flightNumber;
    private String origin;
    private String destination;
    private String seatNumber;
    private String status;
    private String bookingTimestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBookingTimestamp() {
        return bookingTimestamp;
    }

    public void setBookingTimestamp(String bookingTimestamp) {
        this.bookingTimestamp = bookingTimestamp;
    }

    public BookingResponseDto() {
    }

    public BookingResponseDto(Long id, String pnr, String passengerName, String flightNumber,
                              String origin, String destination, String seatNumber, String status,
                              String bookingTimestamp) {
        this.id = id;
        this.pnr = pnr;
        this.passengerName = passengerName;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.seatNumber = seatNumber;
        this.status = status;
        this.bookingTimestamp = bookingTimestamp;
    }
}