package com.skynode.seat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skynode.BusinessConflictException;
import com.skynode.ResourceNotFoundException;
import com.skynode.aircraft.entity.Aircraft;
import com.skynode.aircraft.repository.AircraftRepository;
import com.skynode.seat.Seat;
import com.skynode.seat.dto.SeatRequestDto;
import com.skynode.seat.dto.SeatResponseDto;
import com.skynode.seat.repository.SeatRepository;

@Service
public class SeatService {
    private final AircraftRepository aircraftRepository;
    private final SeatRepository seatRepository;

    public SeatService(AircraftRepository aircraftRepository, SeatRepository seatRepository) {
        this.aircraftRepository = aircraftRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public SeatResponseDto addSeat(Long aircraftId, SeatRequestDto request) {
        Aircraft aircraft = aircraftRepository.findForUpdateById(aircraftId)
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft with id " + aircraftId + " not found"));
        if (seatRepository.existsByAircraftIdAndSeatNumber(aircraftId, request.seatNumber())) {
            throw new BusinessConflictException("Seat " + request.seatNumber() + " already exists on this aircraft");
        }
        Seat seat = seatRepository.save(new Seat(aircraft, request.seatNumber(), request.seatClass()));
        return toResponse(seat);
    }

    public List<SeatResponseDto> getSeats(Long aircraftId) {
        return seatRepository.findByAircraft(findAircraft(aircraftId)).stream().map(this::toResponse).toList();
    }

    public SeatResponseDto getSeat(Long aircraftId, String seatNumber) {
        Seat seat = seatRepository.findByAircraftAndSeatNumber(findAircraft(aircraftId), seatNumber);
        if (seat == null) {
            throw new ResourceNotFoundException("Seat " + seatNumber + " not found on aircraft " + aircraftId);
        }
        return toResponse(seat);
    }

    private Aircraft findAircraft(Long aircraftId) {
        return aircraftRepository.findById(aircraftId)
            .orElseThrow(() -> new ResourceNotFoundException("Aircraft with id " + aircraftId + " not found"));
    }

    private SeatResponseDto toResponse(Seat seat) {
        return new SeatResponseDto(seat.getId(), seat.getAircraft().getId(), seat.getSeatNumber(), seat.getSeatClass());
    }
}
