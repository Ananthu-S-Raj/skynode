
package com.skynode.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.seat.Seat;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pnr;

    @Column(nullable = false)
    private String passengerName;

    @ManyToOne(optional = false)
    private FlightInstance flightInstance;

    @ManyToOne(optional = false)
    private Seat seat;

    @Column(nullable = false)
    private String status; // CONFIRMED or CANCELLED

    @Column(name = "booking_timestamp", nullable = false)
    private LocalDateTime bookingTimestamp;

    public Booking() {
        this.bookingTimestamp = LocalDateTime.now();
    }

    public Booking(String pnr, String passengerName,
                   FlightInstance flightInstance, Seat seat) {
        this.pnr = pnr;
        this.passengerName = passengerName;
        this.flightInstance = flightInstance;
        this.seat = seat;
        this.status = "CONFIRMED";
        this.bookingTimestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public FlightInstance getFlightInstance() {
        return flightInstance;
    }

    public void setFlightInstance(FlightInstance flightInstance) {
        this.flightInstance = flightInstance;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getBookingTimestamp() {
        return bookingTimestamp;
    }

    public void setBookingTimestamp(LocalDateTime bookingTimestamp) {
        this.bookingTimestamp = bookingTimestamp;
    }
}
