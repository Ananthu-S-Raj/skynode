package com.skynode.airport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skynode.airport.entity.Airport;

public interface AirportRepository extends JpaRepository<Airport , Long>{

}
