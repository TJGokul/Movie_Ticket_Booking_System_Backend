package com.softnix.moviebooking.concurrency;

import com.softnix.moviebooking.dto.request.BookSeatRequest;
import com.softnix.moviebooking.dto.request.CreateShowRequest;
import com.softnix.moviebooking.dto.response.BookingResponse;
import com.softnix.moviebooking.dto.response.RefundResponse;
import com.softnix.moviebooking.dto.response.SeatResponse;
import com.softnix.moviebooking.dto.response.ShowResponse;
import com.softnix.moviebooking.entity.Booking;
import com.softnix.moviebooking.entity.BookingStatus;
import com.softnix.moviebooking.entity.Seat;
import com.softnix.moviebooking.exception.BookingAlreadyCancelledException;
import com.softnix.moviebooking.repository.BookingRepository;
import com.softnix.moviebooking.repository.SeatRepository;
import com.softnix.moviebooking.service.BookingService;
import com.softnix.moviebooking.service.ShowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentCancellationIntegrationTest {

    @Autowired
    private ShowService showService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("Concurrency: 10 concurrent threads cancelling the same booking -> Exactly 1 success, 9 conflicts")
    void testConcurrentCancellationSameBooking() throws InterruptedException {
        // 1. Create show and book a seat
        Instant futureShowTime = Instant.now().plus(5, ChronoUnit.DAYS);
        CreateShowRequest showRequest = new CreateShowRequest(
                "Interstellar", futureShowTime, 5, new BigDecimal("250.00")
        );
        ShowResponse show = showService.createShow(showRequest);
        Long showId = show.showId();

        List<SeatResponse> seats = showService.getAvailableSeats(showId);
        Long seatId = seats.get(0).seatId();

        BookingResponse booking = bookingService.bookSeat(new BookSeatRequest(showId, seatId, "CUST-MAIN"));
        Long bookingId = booking.bookingId();

        // 2. Launch 10 concurrent cancellation requests
        int totalThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Exception> otherExceptions = Collections.synchronizedList(new ArrayList<>());
        List<RefundResponse> refunds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RefundResponse refund = bookingService.cancelBooking(bookingId);
                    successCount.incrementAndGet();
                    refunds.add(refund);
                } catch (BookingAlreadyCancelledException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    otherExceptions.add(ex);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Assertions
        assertThat(completed).isTrue();
        assertThat(otherExceptions).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(totalThreads - 1);
        assertThat(refunds).hasSize(1);
        assertThat(refunds.get(0).refundAmount()).isEqualByComparingTo(new BigDecimal("250.00"));

        // Seat must now be available and booking CANCELLED
        Seat seat = seatRepository.findById(seatId).orElseThrow();
        assertThat(seat.getIsBooked()).isFalse();

        Booking cancelledBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}
