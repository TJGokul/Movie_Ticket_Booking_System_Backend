package com.softnix.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.softnix.moviebooking.dto.request.BookSeatRequest;
import com.softnix.moviebooking.dto.response.BookingResponse;
import com.softnix.moviebooking.dto.response.RefundResponse;
import com.softnix.moviebooking.entity.BookingStatus;
import com.softnix.moviebooking.exception.BookingAlreadyCancelledException;
import com.softnix.moviebooking.exception.BookingNotFoundException;
import com.softnix.moviebooking.exception.GlobalExceptionHandler;
import com.softnix.moviebooking.exception.SeatAlreadyBookedException;
import com.softnix.moviebooking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Should return 201 Created on successful booking")
    void shouldBookSeatSuccessfully() throws Exception {
        BookSeatRequest request = new BookSeatRequest(1L, 10L, "CUST-001");
        BookingResponse response = new BookingResponse(
                100L, 1L, "Avatar", Instant.now().plusSeconds(3600), 10L, "A10", "CUST-001",
                BookingStatus.CONFIRMED, new BigDecimal("250.00"), Instant.now(), null
        );

        when(bookingService.bookSeat(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(100))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.seatNumber").value("A10"))
                .andExpect(jsonPath("$.amountPaid").value(250.00));
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Should return 409 Conflict when seat already booked")
    void shouldReturn409WhenSeatAlreadyBooked() throws Exception {
        BookSeatRequest request = new BookSeatRequest(1L, 10L, "CUST-002");

        when(bookingService.bookSeat(any())).thenThrow(new SeatAlreadyBookedException("A10", 1L));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("SEAT_ALREADY_BOOKED"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/cancel - Should return 200 OK and refund details")
    void shouldCancelBookingSuccessfully() throws Exception {
        RefundResponse refundResponse = new RefundResponse(
                100L, 1L, 10L, "A10", "CUST-001", BookingStatus.CANCELLED,
                new BigDecimal("250.00"), Instant.now(), "Booking 100 successfully cancelled."
        );

        when(bookingService.cancelBooking(100L)).thenReturn(refundResponse);

        mockMvc.perform(post("/api/v1/bookings/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(100))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(250.00));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/cancel - Should return 409 Conflict when already cancelled")
    void shouldReturn409WhenCancellingAlreadyCancelledBooking() throws Exception {
        when(bookingService.cancelBooking(100L)).thenThrow(new BookingAlreadyCancelledException(100L));

        mockMvc.perform(post("/api/v1/bookings/100/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("BOOKING_ALREADY_CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{id} - Should return 404 when booking not found")
    void shouldReturn404WhenBookingNotFound() throws Exception {
        when(bookingService.getBookingById(999L)).thenThrow(new BookingNotFoundException(999L));

        mockMvc.perform(get("/api/v1/bookings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("BOOKING_NOT_FOUND"));
    }
}
