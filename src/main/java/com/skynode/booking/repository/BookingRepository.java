package com.skynode.booking.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.skynode.booking.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findForUpdateById(Long id);

    Booking findByPnr(String pnr);

    boolean existsByPnr(String pnr);

    List<Booking> findByPassengerName(String passengerName);
}
