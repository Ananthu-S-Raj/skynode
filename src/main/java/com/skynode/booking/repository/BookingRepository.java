package com.skynode.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skynode.booking.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Booking findByPnr(String pnr);

    boolean existsByPnr(String pnr);

    List<Booking> findByPassengerName(String passengerName);
}