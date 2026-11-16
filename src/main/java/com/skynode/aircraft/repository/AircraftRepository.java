package com.skynode.aircraft.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.skynode.aircraft.entity.Aircraft;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Aircraft a WHERE a.id = :id")
    Optional<Aircraft> findForUpdateById(Long id);
}
