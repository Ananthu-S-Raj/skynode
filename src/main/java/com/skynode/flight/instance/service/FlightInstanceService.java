package com.skynode.flight.instance.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skynode.InvalidRequestException;
import com.skynode.ResourceNotFoundException;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.flight.dto.FlightInstanceRequestDto;
import com.skynode.flight.entity.Flight;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.flight.instance.repository.FlightInstanceRepository;
import com.skynode.flight.repository.FlightRepository;
import com.skynode.seat.FlightSeat;
import com.skynode.seat.Seat;
import com.skynode.seat.repository.FlightSeatRepository;
import com.skynode.seat.repository.SeatRepository;

@Service
public class FlightInstanceService {
    private static final Set<String> SUPPORTED_STATUSES = Set.of(
        "SCHEDULED", "DELAYED", "BOARDING", "DEPARTED", "ARRIVED", "CANCELLED"
    );

    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightRepository flightRepository;
    private final AircraftRepository aircraftRepository;
    private final SeatRepository seatRepository;
    private final FlightSeatRepository flightSeatRepository;

    public FlightInstanceService(FlightInstanceRepository flightInstanceRepository,
                                 FlightRepository flightRepository,
                                 SeatRepository seatRepository,
                                 FlightSeatRepository flightSeatRepository,
                                 AircraftRepository aircraftRepository) {
        this.flightInstanceRepository = flightInstanceRepository;
        this.flightRepository = flightRepository;
        this.aircraftRepository = aircraftRepository;
        this.seatRepository = seatRepository;
        this.flightSeatRepository = flightSeatRepository;
    }

    @Transactional
    public FlightInstance scheduleFlightInstance(FlightInstanceRequestDto request) {
        if (request.getFlightId() == null || request.getFlightId() <= 0) {
            throw new InvalidRequestException("A positive flightId is required");
        }
        String status = request.getStatus() == null ? "SCHEDULED" : request.getStatus();
        validateSchedule(request.getDepartureDateTime(), request.getArrivalDateTime(), status);

        Flight flight = flightRepository.findForUpdateById(request.getFlightId())
            .orElseThrow(() -> new ResourceNotFoundException("Flight with id " + request.getFlightId() + " not found"));

        String departureDateTime = request.getDepartureDateTime();
        String arrivalDateTime = request.getArrivalDateTime();

        FlightInstance flightInstance = new FlightInstance(flight, departureDateTime, arrivalDateTime, status);
        flightInstance = flightInstanceRepository.save(flightInstance);

        Aircraft aircraft = aircraftRepository.findForUpdateById(flight.getAircraft().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found for flight"));
        List<Seat> seats = seatRepository.findByAircraft(aircraft);

        List<FlightSeat> flightSeats = new ArrayList<>();
        for (Seat seat : seats) {
            flightSeats.add(new FlightSeat(seat, flightInstance, "AVAILABLE"));
        }
        flightSeatRepository.saveAll(flightSeats);
        return flightInstance;
    }

    public FlightInstance getInstanceById(Long id) {
        return flightInstanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Flight instance with id " + id + " not found"));
    }

    public List<FlightInstance> getAllInstances() {
        return flightInstanceRepository.findAll();
    }

    public List<FlightInstance> getInstancesByFlightId(Long flightId) {
        return flightInstanceRepository.findByFlightId(flightId);
    }

    @Transactional
    public FlightInstance updateFlightInstance(Long id, FlightInstanceRequestDto request) {
        FlightInstance flightInstance = getInstanceById(id);

        if (request.getFlightId() != null
                && !request.getFlightId().equals(flightInstance.getFlight().getId())) {
            throw new InvalidRequestException("The flight of a scheduled instance cannot be changed");
        }
        String departure = request.getDepartureDateTime() == null
            ? flightInstance.getDepartureDateTime() : request.getDepartureDateTime();
        String arrival = request.getArrivalDateTime() == null
            ? flightInstance.getArrivalDateTime() : request.getArrivalDateTime();
        String status = request.getStatus() == null ? flightInstance.getStatus() : request.getStatus();
        validateSchedule(departure, arrival, status);
        flightInstance.setDepartureDateTime(departure);
        flightInstance.setArrivalDateTime(arrival);
        flightInstance.setStatus(status);

        return flightInstanceRepository.save(flightInstance);
    }

    public void updateFlightStatus(Long id, String status) {
        validateStatus(status);
        FlightInstance flightInstance = getInstanceById(id);
        flightInstance.setStatus(status);
        flightInstanceRepository.save(flightInstance);
    }

    private void validateSchedule(String departure, String arrival, String status) {
        if (departure == null || arrival == null) {
            throw new InvalidRequestException("Departure and arrival date/time are required");
        }
        try {
            LocalDateTime departureTime = LocalDateTime.parse(departure);
            LocalDateTime arrivalTime = LocalDateTime.parse(arrival);
            if (!arrivalTime.isAfter(departureTime)) {
                throw new InvalidRequestException("Arrival must be after departure");
            }
        } catch (DateTimeParseException ex) {
            throw new InvalidRequestException("Date/time must be a valid ISO local date/time, for example 2027-01-15T09:00:00");
        }
        validateStatus(status);
    }

    private void validateStatus(String status) {
        if (status == null || !SUPPORTED_STATUSES.contains(status)) {
            throw new InvalidRequestException("Unsupported status; use SCHEDULED, DELAYED, BOARDING, DEPARTED, ARRIVED or CANCELLED");
        }
    }
}
