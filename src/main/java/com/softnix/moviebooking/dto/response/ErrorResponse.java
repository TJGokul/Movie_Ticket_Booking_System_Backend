package com.softnix.moviebooking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Standardized error response payload")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(description = "Error timestamp in UTC", example = "2026-08-27T10:00:00Z")
    Instant timestamp,

    @Schema(description = "HTTP Status Code", example = "409")
    int status,

    @Schema(description = "High-level error code/type", example = "SEAT_ALREADY_BOOKED")
    String error,

    @Schema(description = "Descriptive error message", example = "Seat A10 is already booked for show 1")
    String message,

    @Schema(description = "Request URI path", example = "/api/v1/bookings")
    String path,

    @Schema(description = "Validation field errors (if applicable)")
    Map<String, String> fieldErrors
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        this(Instant.now(), status, error, message, path, fieldErrors);
    }
}
