package com.skynode.flight.instance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.skynode.flight.instance.entity.FlightInstance;

public interface FlightInstanceRepository extends JpaRepository<FlightInstance, Long> {

    boolean existsByFlightId(Long flightId);

    List<FlightInstance> findByFlightId(Long flightId);

    @Query("SELECT fi FROM FlightInstance fi " +
           "WHERE fi.departureDateTime LIKE :date " +
           "ORDER BY fi.departureDateTime")
    List<FlightInstance> findByDepartureDate(String date);
}
