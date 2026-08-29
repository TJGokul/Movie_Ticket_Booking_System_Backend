package com.softnix.moviebooking.controller;

import com.softnix.moviebooking.dto.request.CreateShowRequest;
import com.softnix.moviebooking.dto.response.BookingResponse;
import com.softnix.moviebooking.dto.response.ErrorResponse;
import com.softnix.moviebooking.dto.response.SeatResponse;
import com.softnix.moviebooking.dto.response.ShowResponse;
import com.softnix.moviebooking.service.BookingService;
import com.softnix.moviebooking.service.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shows")
@Tag(name = "Shows", description = "Operations for managing movie shows and viewing seat availability")
public class ShowController {

    private final ShowService showService;
    private final BookingService bookingService;

    public ShowController(ShowService showService, BookingService bookingService) {
        this.showService = showService;
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new movie show",
            description = "Creates a movie show and automatically generates all seats with initial availability",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Show created successfully",
                            content = @Content(schema = @Schema(implementation = ShowResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error in request payload",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody CreateShowRequest request) {
        ShowResponse response = showService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{showId}")
    @Operation(
            summary = "Get show details by ID",
            description = "Retrieves information about a movie show including current available seats count",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Show details retrieved",
                            content = @Content(schema = @Schema(implementation = ShowResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Show not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<ShowResponse> getShow(@PathVariable Long showId) {
        ShowResponse response = showService.getShowById(showId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "List all movie shows",
            description = "Returns a list of all configured movie shows",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of shows",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ShowResponse.class))))
            }
    )
    public ResponseEntity<List<ShowResponse>> getAllShows() {
        List<ShowResponse> shows = showService.getAllShows();
        return ResponseEntity.ok(shows);
    }

    @GetMapping("/{showId}/available-seats")
    @Operation(
            summary = "Get real-time available seats",
            description = "Returns all seats for the given show where isBooked == false",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of currently available seats",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SeatResponse.class)))),
                    @ApiResponse(responseCode = "404", description = "Show not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable Long showId) {
        List<SeatResponse> seats = showService.getAvailableSeats(showId);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/{showId}/seats")
    @Operation(
            summary = "Get all seats with their booking status",
            description = "Returns all seats for the show with their current booking status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of all seats",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SeatResponse.class)))),
                    @ApiResponse(responseCode = "404", description = "Show not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<List<SeatResponse>> getAllSeats(@PathVariable Long showId) {
        List<SeatResponse> seats = showService.getAllSeatsForShow(showId);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/{showId}/bookings")
    @Operation(
            summary = "Get all bookings for a show",
            description = "Returns all booking history for the specified movie show",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of bookings for the show",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookingResponse.class)))),
                    @ApiResponse(responseCode = "404", description = "Show not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<List<BookingResponse>> getShowBookings(@PathVariable Long showId) {
        List<BookingResponse> bookings = bookingService.getBookingsByShow(showId);
        return ResponseEntity.ok(bookings);
    }
}
