package com.skynode.flight.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.skynode.flight.entity.Flight;
import com.skynode.airport.entity.Airport;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    Flight findByFlightNumber(String flightNumber);

    @Query("SELECT f FROM Flight f WHERE f.origin = :origin AND f.destination = :destination")
    List<Flight> findByOriginAndDestination(Airport origin, Airport destination);
}