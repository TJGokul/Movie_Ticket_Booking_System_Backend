package com.softnix.moviebooking.repository;

import com.softnix.moviebooking.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("SELECT s FROM Show s LEFT JOIN FETCH s.seats WHERE s.showId = :showId")
    Optional<Show> findByIdWithSeats(@Param("showId") Long showId);

    List<Show> findByShowTimeAfterOrderByShowTimeAsc(Instant time);
}
