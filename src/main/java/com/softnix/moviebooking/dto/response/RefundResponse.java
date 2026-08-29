package com.softnix.moviebooking.dto.response;

import com.softnix.moviebooking.entity.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Details of a completed booking cancellation and refund")
public record RefundResponse(
    @Schema(description = "Booking ID that was cancelled", example = "1")
    Long bookingId,

    @Schema(description = "Show ID", example = "1")
    Long showId,

    @Schema(description = "Seat ID that was released", example = "10")
    Long seatId,

    @Schema(description = "Seat Number released", example = "A10")
    String seatNumber,

    @Schema(description = "Customer ID refunded", example = "CUST-1001")
    String customerId,

    @Schema(description = "Updated Booking Status (CANCELLED)", example = "CANCELLED")
    BookingStatus status,

    @Schema(description = "Full refund amount credited back to the customer", example = "250.00")
    BigDecimal refundAmount,

    @Schema(description = "Cancellation & refund timestamp", example = "2026-08-27T10:15:00Z")
    Instant cancelledAt,

    @Schema(description = "Human-readable summary message", example = "Booking 1 successfully cancelled. Full refund of 250.00 processed.")
    String message
) {}
