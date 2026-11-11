package com.skynode.aircraft.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skynode.aircraft.entity.Aircraft;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {

    Aircraft findByRegistrationNumber(String registrationNumber);
}