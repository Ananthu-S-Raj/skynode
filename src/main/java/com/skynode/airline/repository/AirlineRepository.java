package com.skynode.airline.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skynode.airline.entity.Airline;

public interface AirlineRepository extends JpaRepository<Airline, Long> {

}
