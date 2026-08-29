package com.softnix.moviebooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Information about a movie show")
public record ShowResponse(
    @Schema(description = "Show ID", example = "1")
    Long showId,

    @Schema(description = "Movie Name", example = "Avengers: Endgame")
    String movieName,

    @Schema(description = "Show date and time (UTC)", example = "2026-10-31T18:30:00Z")
    Instant showTime,

    @Schema(description = "Total capacity/seats", example = "100")
    Integer totalSeats,

    @Schema(description = "Currently available seats count", example = "98")
    Long availableSeatsCount,

    @Schema(description = "Ticket price per seat", example = "250.00")
    BigDecimal pricePerSeat,

    @Schema(description = "Creation timestamp", example = "2026-08-27T10:00:00Z")
    Instant createdAt,

    @Schema(description = "List of generated seats (included when creating or requesting full layout)")
    List<SeatResponse> seats
) {}
