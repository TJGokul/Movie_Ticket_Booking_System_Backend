package com.softnix.moviebooking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Request payload to create a new movie show and automatically generate seats")
public record CreateShowRequest(

    @Schema(description = "Name/Title of the movie", example = "Avengers: Endgame")
    @NotBlank(message = "Movie name must not be blank")
    @Size(max = 255, message = "Movie name must not exceed 255 characters")
    String movieName,

    @Schema(description = "Show date and time in UTC (ISO-8601)", example = "2026-10-31T18:30:00Z")
    @NotNull(message = "Show time must not be null")
    @Future(message = "Show time must be in the future")
    Instant showTime,

    @Schema(description = "Total number of seats to configure for this show", example = "100")
    @NotNull(message = "Total seats must not be null")
    @Min(value = 1, message = "Total seats must be at least 1")
    @Max(value = 1000, message = "Total seats cannot exceed 1000")
    Integer totalSeats,

    @Schema(description = "Price per seat for this show", example = "250.00")
    @NotNull(message = "Price per seat must not be null")
    @DecimalMin(value = "0.01", message = "Price per seat must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Price per seat format must be valid currency (up to 2 decimals)")
    BigDecimal pricePerSeat
) {}
