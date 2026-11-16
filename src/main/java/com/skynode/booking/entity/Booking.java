
package com.skynode.booking.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    private String passengerEmail;
    private String passengerPhone;
    private Integer passengerAge;
    private String passengerGender;
    private Integer totalAmount;
    private String paymentStatus;

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
        this();
        this.pnr = pnr;
        this.passengerName = passengerName;
        this.flightInstance = flightInstance;
        this.seat = seat;
        this.status = "CONFIRMED";
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

    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String value) { passengerEmail = value; }
    public String getPassengerPhone() { return passengerPhone; }
    public void setPassengerPhone(String value) { passengerPhone = value; }
    public Integer getPassengerAge() { return passengerAge; }
    public void setPassengerAge(Integer value) { passengerAge = value; }
    public String getPassengerGender() { return passengerGender; }
    public void setPassengerGender(String value) { passengerGender = value; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer value) { totalAmount = value; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String value) { paymentStatus = value; }

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
