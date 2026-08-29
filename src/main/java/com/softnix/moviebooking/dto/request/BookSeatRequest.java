package com.softnix.moviebooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to book a single seat for a movie show")
public record BookSeatRequest(

    @Schema(description = "Unique Identifier of the movie show", example = "1")
    @NotNull(message = "Show ID must not be null")
    Long showId,

    @Schema(description = "Unique Identifier of the seat to book", example = "10")
    @NotNull(message = "Seat ID must not be null")
    Long seatId,

    @Schema(description = "Customer ID or identifier requesting the booking", example = "CUST-1001")
    @NotBlank(message = "Customer ID must not be blank")
    @Size(max = 100, message = "Customer ID must not exceed 100 characters")
    String customerId
) {}
