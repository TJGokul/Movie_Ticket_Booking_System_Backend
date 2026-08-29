package com.softnix.moviebooking.repository;

import com.softnix.moviebooking.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s JOIN FETCH s.show WHERE s.seatId = :seatId AND s.show.showId = :showId")
    Optional<Seat> findByIdAndShowIdForUpdate(@Param("seatId") Long seatId, @Param("showId") Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.seatId = :seatId")
    Optional<Seat> findByIdForUpdate(@Param("seatId") Long seatId);

    @Query("SELECT s FROM Seat s JOIN FETCH s.show WHERE s.show.showId = :showId AND s.isBooked = false ORDER BY s.seatNumber ASC")
    List<Seat> findAvailableSeatsByShowId(@Param("showId") Long showId);

    @Query("SELECT s FROM Seat s JOIN FETCH s.show WHERE s.show.showId = :showId ORDER BY s.seatNumber ASC")
    List<Seat> findAllByShowId(@Param("showId") Long showId);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.show.showId = :showId AND s.isBooked = false")
    long countAvailableSeatsByShowId(@Param("showId") Long showId);
}
