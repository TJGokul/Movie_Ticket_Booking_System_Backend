package com.softnix.moviebooking.repository;

import com.softnix.moviebooking.entity.Booking;
import com.softnix.moviebooking.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b JOIN FETCH b.show JOIN FETCH b.seat WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdForUpdate(@Param("bookingId") Long bookingId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.show JOIN FETCH b.seat WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdWithDetails(@Param("bookingId") Long bookingId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.show JOIN FETCH b.seat WHERE b.customerId = :customerId ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.show JOIN FETCH b.seat WHERE b.show.showId = :showId ORDER BY b.createdAt DESC")
    List<Booking> findByShowId(@Param("showId") Long showId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.show.showId = :showId AND b.seat.seatId = :seatId AND b.status IN (:statuses)")
    long countActiveBookings(@Param("showId") Long showId, @Param("seatId") Long seatId, @Param("statuses") List<BookingStatus> statuses);
}
