package com.skynode.booking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skynode.booking.dto.BookingRequestDto;
import com.skynode.booking.dto.BookingResponseDto;
import com.skynode.booking.entity.Booking;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.seat.FlightSeat;
import com.skynode.seat.Seat;
import com.skynode.seat.repository.FlightSeatRepository;
import com.skynode.seat.repository.SeatRepository;
import com.skynode.flight.instance.repository.FlightInstanceRepository;
import com.skynode.booking.repository.BookingRepository;
import com.skynode.ResourceNotFoundException;
import com.skynode.BusinessConflictException;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final SeatRepository seatRepository;
    private final FlightSeatRepository flightSeatRepository;

    public BookingService(BookingRepository bookingRepository,
                          FlightInstanceRepository flightInstanceRepository,
                          SeatRepository seatRepository,
                          FlightSeatRepository flightSeatRepository) {
        this.bookingRepository = bookingRepository;
        this.flightInstanceRepository = flightInstanceRepository;
        this.seatRepository = seatRepository;
        this.flightSeatRepository = flightSeatRepository;
    }

    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request) {
        FlightInstance flightInstance = flightInstanceRepository.findById(request.getFlightInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException("Flight instance with id " + request.getFlightInstanceId() + " not found"));

        Seat seat = seatRepository.findById(request.getSeatId())
            .orElseThrow(() -> new ResourceNotFoundException("Seat with id " + request.getSeatId() + " not found"));

        // Verify the seat belongs to the aircraft assigned to this flight instance
        if (seat.getAircraft() == null || !seat.getAircraft().equals(flightInstance.getFlight().getAircraft())) {
            throw new BusinessConflictException("Seat does not belong to the aircraft assigned to this flight");
        }

        // Retrieve the FlightSeat with PESSIMISTIC_WRITE lock to prevent concurrent double-booking
        FlightSeat flightSeat = flightSeatRepository.findBySeatAndFlightInstance(seat, flightInstance)
            .orElseThrow(() -> new ResourceNotFoundException("Seat is not configured for this flight instance"));

        // Check availability under the lock
        if (!"AVAILABLE".equals(flightSeat.getStatus())) {
            throw new BusinessConflictException("Seat is not available for booking on this flight instance");
        }

        // Generate a unique PNR
        String pnr = generatePnr();

        // Check for duplicate PNR (very unlikely with UUID, but just in case)
        while (bookingRepository.existsByPnr(pnr)) {
            pnr = generatePnr();
        }

        // Book the seat: transition FlightSeat status to BOOKED
        flightSeat.setStatus("BOOKED");
        flightSeatRepository.save(flightSeat);

        // Create the booking
        Booking booking = new Booking(pnr, request.getPassengerName(), flightInstance, seat);
        booking.setPnr(pnr);
        booking = bookingRepository.save(booking);

        return new BookingResponseDto(
            booking.getId(),
            booking.getPnr(),
            booking.getPassengerName(),
            booking.getFlightInstance().getFlight().getFlightNumber(),
            booking.getFlightInstance().getFlight().getOrigin().getName(),
            booking.getFlightInstance().getFlight().getDestination().getName(),
            booking.getSeat().getSeatNumber(),
            booking.getStatus(),
            booking.getBookingTimestamp().toString()
        );
    }

    private String generatePnr() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public BookingResponseDto getBookingById(Long id) {
        Optional<Booking> optional = bookingRepository.findById(id);
        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("Booking with id " + id + " not found");
        }
        Booking booking = optional.get();

        return new BookingResponseDto(
            booking.getId(),
            booking.getPnr(),
            booking.getPassengerName(),
            booking.getFlightInstance().getFlight().getFlightNumber(),
            booking.getFlightInstance().getFlight().getOrigin().getName(),
            booking.getFlightInstance().getFlight().getDestination().getName(),
            booking.getSeat().getSeatNumber(),
            booking.getStatus(),
            booking.getBookingTimestamp().toString()
        );
    }

    public BookingResponseDto getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr);

        if (booking == null) {
            throw new ResourceNotFoundException("Booking with PNR " + pnr + " not found");
        }

        return new BookingResponseDto(
            booking.getId(),
            booking.getPnr(),
            booking.getPassengerName(),
            booking.getFlightInstance().getFlight().getFlightNumber(),
            booking.getFlightInstance().getFlight().getOrigin().getName(),
            booking.getFlightInstance().getFlight().getDestination().getName(),
            booking.getSeat().getSeatNumber(),
            booking.getStatus(),
            booking.getBookingTimestamp().toString()
        );
    }

    public List<BookingResponseDto> getBookingsByPassenger(String passengerName) {
        return bookingRepository.findByPassengerName(passengerName).stream()
            .map(booking -> new BookingResponseDto(
                booking.getId(),
                booking.getPnr(),
                booking.getPassengerName(),
                booking.getFlightInstance().getFlight().getFlightNumber(),
                booking.getFlightInstance().getFlight().getOrigin().getName(),
                booking.getFlightInstance().getFlight().getDestination().getName(),
                booking.getSeat().getSeatNumber(),
                booking.getStatus(),
                booking.getBookingTimestamp().toString()
            )).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public BookingResponseDto cancelBooking(Long id) {
        Optional<Booking> optional = bookingRepository.findById(id);
        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("Booking with id " + id + " not found");
        }
        Booking booking = optional.get();

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BusinessConflictException("Booking is already cancelled");
        }

        // Release the seat: transition FlightSeat status back to AVAILABLE
        FlightSeat flightSeat = flightSeatRepository.findBySeatAndFlightInstance(booking.getSeat(), booking.getFlightInstance())
            .orElseThrow(() -> new ResourceNotFoundException("Seat is not configured for this flight instance"));
        flightSeat.setStatus("AVAILABLE");
        flightSeatRepository.save(flightSeat);

        booking.setStatus("CANCELLED");
        booking = bookingRepository.save(booking);

        return new BookingResponseDto(
            booking.getId(),
            booking.getPnr(),
            booking.getPassengerName(),
            booking.getFlightInstance().getFlight().getFlightNumber(),
            booking.getFlightInstance().getFlight().getOrigin().getName(),
            booking.getFlightInstance().getFlight().getDestination().getName(),
            booking.getSeat().getSeatNumber(),
            booking.getStatus(),
            booking.getBookingTimestamp().toString()
        );
    }
}