package com.skynode.flight.instance.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skynode.aircraft.entity.Aircraft;
import com.skynode.flight.dto.FlightInstanceRequestDto;
import com.skynode.flight.entity.Flight;
import com.skynode.flight.repository.FlightRepository;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.flight.instance.repository.FlightInstanceRepository;
import com.skynode.ResourceNotFoundException;
import com.skynode.seat.FlightSeat;
import com.skynode.seat.Seat;
import com.skynode.seat.repository.FlightSeatRepository;
import com.skynode.seat.repository.SeatRepository;

@Service
public class FlightInstanceService {

    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final FlightSeatRepository flightSeatRepository;

    public FlightInstanceService(FlightInstanceRepository flightInstanceRepository,
                                 FlightRepository flightRepository,
                                 SeatRepository seatRepository,
                                 FlightSeatRepository flightSeatRepository) {
        this.flightInstanceRepository = flightInstanceRepository;
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
        this.flightSeatRepository = flightSeatRepository;
    }

    @Transactional
    public FlightInstance scheduleFlightInstance(FlightInstanceRequestDto request) {
        Flight flight = flightRepository.findById(request.getFlightId())
            .orElseThrow(() -> new ResourceNotFoundException("Flight with id " + request.getFlightId() + " not found"));

        String departureDateTime = request.getDepartureDateTime();
        String arrivalDateTime = request.getArrivalDateTime();
        String status = request.getStatus() != null ? request.getStatus() : "SCHEDULED";

        FlightInstance flightInstance = new FlightInstance(flight, departureDateTime, arrivalDateTime, status);
        flightInstance = flightInstanceRepository.save(flightInstance);

        // Initialize FlightSeat availability for each seat of the aircraft
        initializeFlightSeats(flightInstance, flight.getAircraft());

        return flightInstance;
    }

    private void initializeFlightSeats(FlightInstance flightInstance, Aircraft aircraft) {
        if (aircraft == null) {
            throw new ResourceNotFoundException("Aircraft not found for flight instance");
        }
        List<Seat> seats = seatRepository.findByAircraft(aircraft);

        List<FlightSeat> flightSeats = new java.util.ArrayList<>();
        for (Seat seat : seats) {
            flightSeats.add(new FlightSeat(seat, flightInstance, "AVAILABLE"));
        }
        flightSeatRepository.saveAll(flightSeats);
    }

    public FlightInstance getInstanceById(Long id) {
        return flightInstanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Flight instance with id " + id + " not found"));
    }

    public List<FlightInstance> getInstancesByFlightId(Long flightId) {
        return flightInstanceRepository.findByFlightId(flightId);
    }

    public FlightInstance updateFlightInstance(Long id, FlightInstanceRequestDto request) {
        FlightInstance flightInstance = flightInstanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Flight instance with id " + id + " not found"));

        if (request.getDepartureDateTime() != null) {
            flightInstance.setDepartureDateTime(request.getDepartureDateTime());
        }
        if (request.getArrivalDateTime() != null) {
            flightInstance.setArrivalDateTime(request.getArrivalDateTime());
        }
        if (request.getStatus() != null) {
            flightInstance.setStatus(request.getStatus());
        }

        return flightInstanceRepository.save(flightInstance);
    }

    public void updateFlightStatus(Long id, String status) {
        FlightInstance flightInstance = flightInstanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Flight instance with id " + id + " not found"));
        flightInstance.setStatus(status);
        flightInstanceRepository.save(flightInstance);
    }
}