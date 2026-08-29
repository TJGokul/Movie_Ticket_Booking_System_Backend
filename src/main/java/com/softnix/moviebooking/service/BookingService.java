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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository,
                          SeatRepository seatRepository,
                          ShowRepository showRepository,
                          Clock clock) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.clock = clock;
    }

    /**
     * Books a single seat for a given show.
     * Uses pessimistic write row locking to guarantee zero double-booking under concurrent access.
     */
    @Transactional
    public BookingResponse bookSeat(BookSeatRequest request) {
        log.info("Processing booking request: showId={}, seatId={}, customerId='{}'",
                request.showId(), request.seatId(), request.customerId());

        // 1. Verify Show exists
        Show show = showRepository.findById(request.showId())
                .orElseThrow(() -> new ShowNotFoundException(request.showId()));

        // 2. Validate show has not already started
        Instant now = clock.instant();
        if (!now.isBefore(show.getShowTime())) {
            log.warn("Booking rejected: Show {} has already started or passed at {}", show.getShowId(), show.getShowTime());
            throw new ShowAlreadyStartedException(show.getShowId(), "book a seat");
        }

        // 3. Acquire pessimistic write lock on Seat row (SELECT ... FOR UPDATE)
        Seat seat = seatRepository.findByIdAndShowIdForUpdate(request.seatId(), request.showId())
                .orElseThrow(() -> new SeatNotFoundException(request.seatId(), request.showId()));

        // 4. Validate seat availability
        if (Boolean.TRUE.equals(seat.getIsBooked())) {
            log.warn("Booking conflict: Seat {} (ID: {}) for show {} is already booked.",
                    seat.getSeatNumber(), seat.getSeatId(), show.getShowId());
            throw new SeatAlreadyBookedException(seat.getSeatNumber(), show.getShowId());
        }

        // 5. Atomic state update: Mark seat as booked
        seat.setIsBooked(true);
        seatRepository.save(seat);

        // 6. Create booking with status CONFIRMED (flow CREATED -> CONFIRMED)
        Booking booking = new Booking();
        booking.setShow(show);
        booking.setSeat(seat);
        booking.setCustomerId(request.customerId().trim());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAmountPaid(show.getPricePerSeat());

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Seat {} booked successfully! BookingId: {}, AmountPaid: {}",
                seat.getSeatNumber(), savedBooking.getBookingId(), savedBooking.getAmountPaid());

        return mapToBookingResponse(savedBooking);
    }

    /**
     * Cancels an existing booking before the show start time.
     * Releases the seat immediately and issues a 100% full refund.
     */
    @Transactional
    public RefundResponse cancelBooking(Long bookingId) {
        log.info("Processing cancellation request for bookingId: {}", bookingId);

        // 1. Acquire pessimistic write lock on Booking row
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // 2. Validate current booking status
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Cancellation rejected: Booking {} is already CANCELLED.", bookingId);
            throw new BookingAlreadyCancelledException(bookingId);
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            log.warn("Cancellation rejected: Booking {} is in invalid state: {}", bookingId, booking.getStatus());
            throw new InvalidBookingStateException("Only CONFIRMED bookings can be cancelled. Current status: " + booking.getStatus());
        }

        // 3. Validate show time constraint (must be before showTime)
        Instant now = clock.instant();
        Show show = booking.getShow();
        if (!now.isBefore(show.getShowTime())) {
            log.warn("Cancellation rejected: Show {} started at {}. Current time is {}",
                    show.getShowId(), show.getShowTime(), now);
            throw new ShowAlreadyStartedException("Cancellation not allowed after show start time (" + show.getShowTime() + ")");
        }

        // 4. Acquire pessimistic lock on the associated seat and release it immediately
        Seat seat = seatRepository.findByIdForUpdate(booking.getSeat().getSeatId())
                .orElse(booking.getSeat());
        seat.setIsBooked(false);
        seatRepository.save(seat);

        // 5. Update booking status to CANCELLED and record timestamp
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        bookingRepository.save(booking);

        log.info("Booking {} cancelled successfully. Seat {} released. Full refund of {} processed.",
                bookingId, seat.getSeatNumber(), booking.getAmountPaid());

        String message = String.format("Booking %d successfully cancelled. Full refund of %s processed.",
                bookingId, booking.getAmountPaid().toPlainString());

        return new RefundResponse(
                booking.getBookingId(),
                show.getShowId(),
                seat.getSeatId(),
                seat.getSeatNumber(),
                booking.getCustomerId(),
                booking.getStatus(),
                booking.getAmountPaid(),
                booking.getCancelledAt(),
                message
        );
    }

    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        return mapToBookingResponse(booking);
    }

    public List<BookingResponse> getBookingsByCustomer(String customerId) {
        return bookingRepository.findByCustomerId(customerId.trim()).stream()
                .map(this::mapToBookingResponse)
                .toList();
    }

    public List<BookingResponse> getBookingsByShow(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ShowNotFoundException(showId);
        }
        return bookingRepository.findByShowId(showId).stream()
                .map(this::mapToBookingResponse)
                .toList();
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getBookingId(),
                booking.getShow().getShowId(),
                booking.getShow().getMovieName(),
                booking.getShow().getShowTime(),
                booking.getSeat().getSeatId(),
                booking.getSeat().getSeatNumber(),
                booking.getCustomerId(),
                booking.getStatus(),
                booking.getAmountPaid(),
                booking.getCreatedAt(),
                booking.getCancelledAt()
        );
    }
}
