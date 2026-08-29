package com.softnix.moviebooking.concurrency;

import com.softnix.moviebooking.dto.request.BookSeatRequest;
import com.softnix.moviebooking.dto.request.CreateShowRequest;
import com.softnix.moviebooking.dto.response.BookingResponse;
import com.softnix.moviebooking.dto.response.SeatResponse;
import com.softnix.moviebooking.dto.response.ShowResponse;
import com.softnix.moviebooking.entity.BookingStatus;
import com.softnix.moviebooking.entity.Seat;
import com.softnix.moviebooking.exception.SeatAlreadyBookedException;
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
class ConcurrentBookingIntegrationTest {

    @Autowired
    private ShowService showService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("Concurrency: 20 concurrent threads booking the same seat -> Exactly 1 success, 19 conflicts")
    void testConcurrentBookingSameSeat() throws InterruptedException {
        // 1. Create Show
        Instant futureShowTime = Instant.now().plus(7, ChronoUnit.DAYS);
        CreateShowRequest showRequest = new CreateShowRequest(
                "Oppenheimer", futureShowTime, 10, new BigDecimal("300.00")
        );
        ShowResponse createdShow = showService.createShow(showRequest);
        Long showId = createdShow.showId();

        List<SeatResponse> seats = showService.getAvailableSeats(showId);
        Long targetSeatId = seats.get(0).seatId();

        // 2. Prepare 20 concurrent threads
        int totalThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Exception> otherExceptions = Collections.synchronizedList(new ArrayList<>());
        List<BookingResponse> successfulBookings = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i <= totalThreads; i++) {
            final String customerId = "CUST-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready to fire simultaneously
                    BookingResponse response = bookingService.bookSeat(
                            new BookSeatRequest(showId, targetSeatId, customerId)
                    );
                    successCount.incrementAndGet();
                    successfulBookings.add(response);
                } catch (SeatAlreadyBookedException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    otherExceptions.add(ex);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads concurrently
        startLatch.countDown();
        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Assertions
        assertThat(completed).isTrue();
        assertThat(otherExceptions).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(totalThreads - 1);
        assertThat(successfulBookings).hasSize(1);

        // Verify database state integrity
        Seat bookedSeat = seatRepository.findById(targetSeatId).orElseThrow();
        assertThat(bookedSeat.getIsBooked()).isTrue();

        long activeBookings = bookingRepository.countActiveBookings(
                showId, targetSeatId, List.of(BookingStatus.CREATED, BookingStatus.CONFIRMED)
        );
        assertThat(activeBookings).isEqualTo(1);
    }
}
