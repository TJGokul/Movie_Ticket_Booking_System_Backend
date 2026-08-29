package com.softnix.moviebooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about a seat in a movie show")
public record SeatResponse(
    @Schema(description = "Seat ID", example = "10")
    Long seatId,

    @Schema(description = "Show ID", example = "1")
    Long showId,

    @Schema(description = "Seat designation number/label (e.g., A1, B5)", example = "A10")
    String seatNumber,

    @Schema(description = "Whether the seat is currently booked", example = "false")
    Boolean isBooked
) {}
