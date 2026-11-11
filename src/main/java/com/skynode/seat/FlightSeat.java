package com.skynode.seat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.skynode.flight.instance.entity.FlightInstance;
import com.skynode.seat.Seat;

@Entity
@Table(name = "flight_seats", uniqueConstraints = {
    @UniqueConstraint(name = "uk_flight_seat_instance", columnNames = {"seat_id", "flight_instance_id"})
})
public class FlightSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Seat seat;

    @ManyToOne(optional = false)
    private FlightInstance flightInstance;

    @Column(nullable = false)
    private String status; // AVAILABLE or BOOKED

    public FlightSeat() {
    }

    public FlightSeat(Seat seat, FlightInstance flightInstance, String status) {
        this.seat = seat;
        this.flightInstance = flightInstance;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public FlightInstance getFlightInstance() {
        return flightInstance;
    }

    public void setFlightInstance(FlightInstance flightInstance) {
        this.flightInstance = flightInstance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}