package com.skynode.seat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import com.skynode.seat.FlightSeat;
import com.skynode.seat.Seat;
import com.skynode.flight.instance.entity.FlightInstance;
public interface FlightSeatRepository extends JpaRepository<FlightSeat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FlightSeat> findBySeatAndFlightInstance(Seat seat, FlightInstance flightInstance);

    boolean existsBySeatAndFlightInstance(Seat seat, FlightInstance flightInstance);

    List<FlightSeat> findByFlightInstance(FlightInstance flightInstance);

    long countByFlightInstanceAndStatus(FlightInstance flightInstance, String status);
}
