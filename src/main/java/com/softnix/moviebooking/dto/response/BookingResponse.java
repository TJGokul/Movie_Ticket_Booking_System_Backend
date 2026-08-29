package com.softnix.moviebooking.dto.response;

import com.softnix.moviebooking.entity.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Details of a movie ticket booking")
public record BookingResponse(
    @Schema(description = "Booking ID", example = "1")
    Long bookingId,

    @Schema(description = "Show ID", example = "1")
    Long showId,

    @Schema(description = "Movie Name", example = "Avengers: Endgame")
    String movieName,

    @Schema(description = "Show date and time", example = "2026-10-31T18:30:00Z")
    Instant showTime,

    @Schema(description = "Seat ID", example = "10")
    Long seatId,

    @Schema(description = "Seat Number", example = "A10")
    String seatNumber,

    @Schema(description = "Customer ID", example = "CUST-1001")
    String customerId,

    @Schema(description = "Current Booking Status (CREATED, CONFIRMED, CANCELLED)", example = "CONFIRMED")
    BookingStatus status,

    @Schema(description = "Amount paid for the booking", example = "250.00")
    BigDecimal amountPaid,

    @Schema(description = "Booking creation timestamp", example = "2026-08-27T10:00:00Z")
    Instant createdAt,

    @Schema(description = "Cancellation timestamp (if cancelled)", example = "null")
    Instant cancelledAt
) {}
