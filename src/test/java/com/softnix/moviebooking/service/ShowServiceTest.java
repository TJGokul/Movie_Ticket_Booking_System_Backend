package com.softnix.moviebooking.service;

import com.softnix.moviebooking.dto.request.CreateShowRequest;
import com.softnix.moviebooking.dto.response.SeatResponse;
import com.softnix.moviebooking.dto.response.ShowResponse;
import com.softnix.moviebooking.entity.Seat;
import com.softnix.moviebooking.entity.Show;
import com.softnix.moviebooking.exception.ShowNotFoundException;
import com.softnix.moviebooking.repository.SeatRepository;
import com.softnix.moviebooking.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private ShowService showService;

    private CreateShowRequest createShowRequest;
    private Show mockShow;

    @BeforeEach
    void setUp() {
        Instant showTime = Instant.now().plus(2, ChronoUnit.DAYS);
        createShowRequest = new CreateShowRequest("Inception", showTime, 20, new BigDecimal("250.00"));

        mockShow = new Show(1L, "Inception", showTime, 20, new BigDecimal("250.00"));
        mockShow.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should create show and automatically generate seats")
    void shouldCreateShowWithGeneratedSeats() {
        when(showRepository.save(any(Show.class))).thenAnswer(invocation -> {
            Show showToSave = invocation.getArgument(0);
            showToSave.setShowId(1L);
            long id = 1;
            for (Seat seat : showToSave.getSeats()) {
                seat.setSeatId(id++);
            }
            return showToSave;
        });

        ShowResponse response = showService.createShow(createShowRequest);

        assertThat(response).isNotNull();
        assertThat(response.showId()).isEqualTo(1L);
        assertThat(response.movieName()).isEqualTo("Inception");
        assertThat(response.totalSeats()).isEqualTo(20);
        assertThat(response.seats()).hasSize(20);
        assertThat(response.seats().get(0).seatNumber()).isEqualTo("A1");
        assertThat(response.seats().get(9).seatNumber()).isEqualTo("A10");
        assertThat(response.seats().get(10).seatNumber()).isEqualTo("B1");
        verify(showRepository, times(1)).save(any(Show.class));
    }

    @Test
    @DisplayName("Should return available seats for a show")
    void shouldReturnAvailableSeats() {
        when(showRepository.existsById(1L)).thenReturn(true);
        Seat seat1 = new Seat(mockShow, "A1", false);
        seat1.setSeatId(101L);
        Seat seat2 = new Seat(mockShow, "A2", false);
        seat2.setSeatId(102L);

        when(seatRepository.findAvailableSeatsByShowId(1L)).thenReturn(List.of(seat1, seat2));

        List<SeatResponse> availableSeats = showService.getAvailableSeats(1L);

        assertThat(availableSeats).hasSize(2);
        assertThat(availableSeats.get(0).seatNumber()).isEqualTo("A1");
        assertThat(availableSeats.get(0).isBooked()).isFalse();
    }

    @Test
    @DisplayName("Should throw ShowNotFoundException when show does not exist")
    void shouldThrowWhenShowNotFound() {
        when(showRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> showService.getAvailableSeats(999L))
                .isInstanceOf(ShowNotFoundException.class)
                .hasMessageContaining("999");
    }
}
