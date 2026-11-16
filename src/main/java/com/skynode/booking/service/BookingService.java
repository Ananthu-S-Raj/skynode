package com.skynode.booking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skynode.BusinessConflictException;
import com.skynode.ResourceNotFoundException;
import com.skynode.booking.dto.BookingRequestDto;
import com.skynode.booking.dto.BookingResponseDto;
import com.skynode.booking.entity.Booking;
import com.skynode.booking.repository.BookingRepository;
import com.skynode.flight.entity.Flight;
import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.flight.instance.repository.FlightInstanceRepository;
import com.skynode.seat.FlightSeat;
import com.skynode.seat.Seat;
import com.skynode.seat.repository.FlightSeatRepository;
import com.skynode.seat.repository.SeatRepository;

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

        if (List.of("CANCELLED", "DEPARTED", "ARRIVED").contains(flightInstance.getStatus())) {
            throw new BusinessConflictException("This departure is not open for booking");
        }

        Seat seat = seatRepository.findById(request.getSeatId())
            .orElseThrow(() -> new ResourceNotFoundException("Seat with id " + request.getSeatId() + " not found"));

        if (!seat.getAircraft().getId().equals(flightInstance.getFlight().getAircraft().getId())) {
            throw new BusinessConflictException("Seat does not belong to the aircraft assigned to this flight");
        }

        // Check availability while holding the row lock to prevent double-booking.
        FlightSeat flightSeat = flightSeatRepository.findBySeatAndFlightInstance(seat, flightInstance)
            .orElseThrow(() -> new ResourceNotFoundException("Seat is not configured for this flight instance"));

        if (!"AVAILABLE".equals(flightSeat.getStatus())) {
            throw new BusinessConflictException("Seat is not available for booking on this flight instance");
        }

        String pnr = generatePnr();

        while (bookingRepository.existsByPnr(pnr)) {
            pnr = generatePnr();
        }

        flightSeat.setStatus("BOOKED");
        flightSeatRepository.save(flightSeat);

        Booking booking = new Booking(pnr, request.getPassengerName(), flightInstance, seat);
        booking = bookingRepository.save(booking);

        return toResponse(booking);
    }

    @Transactional
    public BookingResponseDto createDemoBooking(BookingRequestDto request, String email, String phone,
                                                Integer age, String gender, Integer amount) {
        BookingResponseDto created = createBooking(request);
        Booking booking = bookingRepository.findById(created.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        booking.setPassengerEmail(email);
        booking.setPassengerPhone(phone);
        booking.setPassengerAge(age);
        booking.setPassengerGender(gender);
        booking.setTotalAmount(amount);
        booking.setPaymentStatus("PAID_DEMO");
        bookingRepository.save(booking);
        return toResponse(booking);
    }

    private String generatePnr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public BookingResponseDto getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking with id " + id + " not found"));

        return toResponse(booking);
    }

    public BookingResponseDto getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr);

        if (booking == null) {
            throw new ResourceNotFoundException("Booking with PNR " + pnr + " not found");
        }

        return toResponse(booking);
    }

    public List<BookingResponseDto> getBookingsByPassenger(String passengerName) {
        List<BookingResponseDto> response = new ArrayList<>();
        for (Booking booking : bookingRepository.findByPassengerName(passengerName)) {
            response.add(toResponse(booking));
        }
        return response;
    }

    @Transactional
    public BookingResponseDto cancelBooking(Long id) {
        // Serialize cancellations before checking status or releasing the seat.
        Booking booking = bookingRepository.findForUpdateById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking with id " + id + " not found"));

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BusinessConflictException("Booking is already cancelled");
        }

        FlightSeat flightSeat = flightSeatRepository.findBySeatAndFlightInstance(booking.getSeat(), booking.getFlightInstance())
            .orElseThrow(() -> new ResourceNotFoundException("Seat is not configured for this flight instance"));
        flightSeat.setStatus("AVAILABLE");
        flightSeatRepository.save(flightSeat);

        booking.setStatus("CANCELLED");
        booking = bookingRepository.save(booking);

        return toResponse(booking);
    }

    private BookingResponseDto toResponse(Booking booking) {
        Flight flight = booking.getFlightInstance().getFlight();
        BookingResponseDto response = new BookingResponseDto(
            booking.getId(),
            booking.getPnr(),
            booking.getPassengerName(),
            flight.getFlightNumber(),
            flight.getOrigin().getName(),
            flight.getDestination().getName(),
            booking.getSeat().getSeatNumber(),
            booking.getStatus(),
            booking.getBookingTimestamp().toString()
        );
        response.setDepartureDateTime(booking.getFlightInstance().getDepartureDateTime());
        response.setArrivalDateTime(booking.getFlightInstance().getArrivalDateTime());
        response.setPassengerEmail(booking.getPassengerEmail());
        response.setPassengerPhone(booking.getPassengerPhone());
        response.setPassengerAge(booking.getPassengerAge());
        response.setPassengerGender(booking.getPassengerGender());
        response.setTotalAmount(booking.getTotalAmount());
        response.setPaymentStatus(booking.getPaymentStatus());
        return response;
    }
}
