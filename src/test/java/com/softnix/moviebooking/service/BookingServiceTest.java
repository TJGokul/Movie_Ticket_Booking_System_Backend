package com.softnix.moviebooking.service;

import com.softnix.moviebooking.dto.request.BookSeatRequest;
import com.softnix.moviebooking.dto.response.BookingResponse;
import com.softnix.moviebooking.dto.response.RefundResponse;
import com.softnix.moviebooking.entity.Booking;
import com.softnix.moviebooking.entity.BookingStatus;
import com.softnix.moviebooking.entity.Seat;
import com.softnix.moviebooking.entity.Show;
import com.softnix.moviebooking.exception.*;
import com.softnix.moviebooking.repository.BookingRepository;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowRepository showRepository;

    private Clock clock;
    private BookingService bookingService;
    private Instant fixedNow;
    private Show mockShow;
    private Seat mockSeat;

    @BeforeEach
    void setUp() {
        fixedNow = Instant.parse("2026-08-27T10:00:00Z");
        clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        bookingService = new BookingService(bookingRepository, seatRepository, showRepository, clock);

        Instant futureShowTime = fixedNow.plus(2, ChronoUnit.DAYS);
        mockShow = new Show(1L, "Interstellar", futureShowTime, 50, new BigDecimal("300.00"));
        mockSeat = new Seat(mockShow, "A1", false);
        mockSeat.setSeatId(10L);
    }

    @Test
    @DisplayName("Should successfully book an available seat")
    void shouldBookAvailableSeat() {
        BookSeatRequest request = new BookSeatRequest(1L, 10L, "CUST-001");

        when(showRepository.findById(1L)).thenReturn(Optional.of(mockShow));
        when(seatRepository.findByIdAndShowIdForUpdate(10L, 1L)).thenReturn(Optional.of(mockSeat));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setBookingId(100L);
            booking.setCreatedAt(fixedNow);
            return booking;
        });

        BookingResponse response = bookingService.bookSeat(request);

        assertThat(response).isNotNull();
        assertThat(response.bookingId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.amountPaid()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.seatNumber()).isEqualTo("A1");
        assertThat(mockSeat.getIsBooked()).isTrue();

        verify(seatRepository).save(mockSeat);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should reject booking when seat is already booked (409)")
    void shouldRejectWhenSeatAlreadyBooked() {
        mockSeat.setIsBooked(true);
        BookSeatRequest request = new BookSeatRequest(1L, 10L, "CUST-002");

        when(showRepository.findById(1L)).thenReturn(Optional.of(mockShow));
        when(seatRepository.findByIdAndShowIdForUpdate(10L, 1L)).thenReturn(Optional.of(mockSeat));

        assertThatThrownBy(() -> bookingService.bookSeat(request))
                .isInstanceOf(SeatAlreadyBookedException.class)
                .hasMessageContaining("A1");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject booking after show time has passed")
    void shouldRejectBookingAfterShowTime() {
        Instant pastShowTime = fixedNow.minus(1, ChronoUnit.HOURS);
        mockShow.setShowTime(pastShowTime);
        BookSeatRequest request = new BookSeatRequest(1L, 10L, "CUST-003");

        when(showRepository.findById(1L)).thenReturn(Optional.of(mockShow));

        assertThatThrownBy(() -> bookingService.bookSeat(request))
                .isInstanceOf(ShowAlreadyStartedException.class);
    }

    @Test
    @DisplayName("Should cancel confirmed booking before show time, release seat, and refund full amount")
    void shouldCancelBookingAndRefund() {
        Booking confirmedBooking = new Booking(mockShow, mockSeat, "CUST-001", BookingStatus.CONFIRMED, new BigDecimal("300.00"));
        confirmedBooking.setBookingId(100L);
        mockSeat.setIsBooked(true);

        when(bookingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(confirmedBooking));
        when(seatRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mockSeat));
        when(bookingRepository.save(any(Booking.class))).thenReturn(confirmedBooking);

        RefundResponse refundResponse = bookingService.cancelBooking(100L);

        assertThat(refundResponse).isNotNull();
        assertThat(refundResponse.bookingId()).isEqualTo(100L);
        assertThat(refundResponse.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(refundResponse.refundAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(mockSeat.getIsBooked()).isFalse();

        verify(seatRepository).save(mockSeat);
        verify(bookingRepository).save(confirmedBooking);
    }

    @Test
    @DisplayName("Should reject cancellation when booking is already CANCELLED (409)")
    void shouldRejectDuplicateCancellation() {
        Booking cancelledBooking = new Booking(mockShow, mockSeat, "CUST-001", BookingStatus.CANCELLED, new BigDecimal("300.00"));
        cancelledBooking.setBookingId(100L);

        when(bookingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(cancelledBooking));

        assertThatThrownBy(() -> bookingService.cancelBooking(100L))
                .isInstanceOf(BookingAlreadyCancelledException.class)
                .hasMessageContaining("100");
    }

    @Test
    @DisplayName("Should reject cancellation after show start time (409)")
    void shouldRejectCancellationAfterShowTime() {
        Instant pastShowTime = fixedNow.minus(30, ChronoUnit.MINUTES);
        mockShow.setShowTime(pastShowTime);

        Booking booking = new Booking(mockShow, mockSeat, "CUST-001", BookingStatus.CONFIRMED, new BigDecimal("300.00"));
        booking.setBookingId(100L);

        when(bookingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(100L))
                .isInstanceOf(ShowAlreadyStartedException.class);
    }
}
