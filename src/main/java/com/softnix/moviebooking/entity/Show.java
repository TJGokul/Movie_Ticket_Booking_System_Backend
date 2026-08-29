package com.softnix.moviebooking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_id")
    private Long showId;

    @Column(name = "movie_name", nullable = false)
    private String movieName;

    @Column(name = "show_time", nullable = false)
    private Instant showTime;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "price_per_seat", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "show", fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    public Show() {
    }

    public Show(Long showId, String movieName, Instant showTime, Integer totalSeats, BigDecimal pricePerSeat) {
        this.showId = showId;
        this.movieName = movieName;
        this.showTime = showTime;
        this.totalSeats = totalSeats;
        this.pricePerSeat = pricePerSeat;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
        seat.setShow(this);
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public Instant getShowTime() {
        return showTime;
    }

    public void setShowTime(Instant showTime) {
        this.showTime = showTime;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public BigDecimal getPricePerSeat() {
        return pricePerSeat;
    }

    public void setPricePerSeat(BigDecimal pricePerSeat) {
        this.pricePerSeat = pricePerSeat;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }
}
