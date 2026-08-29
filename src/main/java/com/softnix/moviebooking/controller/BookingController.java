package com.softnix.moviebooking.controller;

import com.softnix.moviebooking.dto.request.BookSeatRequest;
import com.softnix.moviebooking.dto.response.BookingResponse;
import com.softnix.moviebooking.dto.response.ErrorResponse;
import com.softnix.moviebooking.dto.response.RefundResponse;
import com.softnix.moviebooking.service.BookingService;
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
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Operations for booking movie seats, retrieving booking details, and processing cancellations")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(
            summary = "Book a single seat for a show",
            description = "Atomically books a seat per request with pessimistic row locking, guaranteeing zero double-booking",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Seat booked successfully",
                            content = @Content(schema = @Schema(implementation = BookingResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Show or seat not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Seat already booked or show already started",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<BookingResponse> bookSeat(@Valid @RequestBody BookSeatRequest request) {
        BookingResponse response = bookingService.bookSeat(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(
            summary = "Cancel a booking and receive a full refund",
            description = "Cancels a confirmed booking before show start time, releases the seat immediately, and refunds the full amount paid",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Booking successfully cancelled and refund issued",
                            content = @Content(schema = @Schema(implementation = RefundResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Booking not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Booking already cancelled or show has already started",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<RefundResponse> cancelBooking(@PathVariable Long bookingId) {
        RefundResponse response = bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}")
    @Operation(
            summary = "Get booking details by ID",
            description = "Retrieves information about a specific booking including its status and payment amount",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Booking details retrieved",
                            content = @Content(schema = @Schema(implementation = BookingResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Booking not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long bookingId) {
        BookingResponse response = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Get bookings by customer ID",
            description = "Retrieves all bookings made by a specific customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of bookings for customer",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookingResponse.class))))
            }
    )
    public ResponseEntity<List<BookingResponse>> getBookingsByCustomer(@RequestParam String customerId) {
        List<BookingResponse> responses = bookingService.getBookingsByCustomer(customerId);
        return ResponseEntity.ok(responses);
    }
}
