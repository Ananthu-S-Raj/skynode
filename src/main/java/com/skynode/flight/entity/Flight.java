package com.skynode.flight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.skynode.aircraft.entity.Aircraft;
import com.skynode.airline.entity.Airline;
import com.skynode.airport.entity.Airport;

@Entity
@Table(name = "flights")
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Airline airline;

    @ManyToOne(optional = false)
    private Airport origin;

    @ManyToOne(optional = false)
    private Airport destination;

    @ManyToOne(optional = false)
    private Aircraft aircraft;

    @Column(nullable = false)
    private String flightNumber;

    public Flight() {
    }

    public Flight(Airline airline, Airport origin, Airport destination, Aircraft aircraft, String flightNumber) {
        this.airline = airline;
        this.origin = origin;
        this.destination = destination;
        this.aircraft = aircraft;
        this.flightNumber = flightNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Airline getAirline() {
        return airline;
    }

    public void setAirline(Airline airline) {
        this.airline = airline;
    }

    public Airport getOrigin() {
        return origin;
    }

    public void setOrigin(Airport origin) {
        this.origin = origin;
    }

    public Airport getDestination() {
        return destination;
    }

    public void setDestination(Airport destination) {
        this.destination = destination;
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }
}
