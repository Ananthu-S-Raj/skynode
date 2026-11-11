package com.skynode.seat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skynode.aircraft.entity.Aircraft;
import com.skynode.seat.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    Seat findByAircraftAndSeatNumber(Aircraft aircraft, String seatNumber);

    List<Seat> findByAircraft(Aircraft aircraft);

    long countByAircraft(Aircraft aircraft);
}