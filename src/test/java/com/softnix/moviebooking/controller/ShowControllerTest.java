package com.softnix.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.softnix.moviebooking.dto.request.CreateShowRequest;
import com.softnix.moviebooking.dto.response.SeatResponse;
import com.softnix.moviebooking.dto.response.ShowResponse;
import com.softnix.moviebooking.exception.GlobalExceptionHandler;
import com.softnix.moviebooking.exception.ShowNotFoundException;
import com.softnix.moviebooking.service.BookingService;
import com.softnix.moviebooking.service.ShowService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ShowControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ShowService showService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private ShowController showController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(showController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/shows - Should return 201 Created on valid request")
    void shouldCreateShow() throws Exception {
        Instant showTime = Instant.now().plus(3, ChronoUnit.DAYS);
        CreateShowRequest request = new CreateShowRequest("Dune Part Two", showTime, 10, new BigDecimal("350.00"));
        ShowResponse response = new ShowResponse(1L, "Dune Part Two", showTime, 10, 10L, new BigDecimal("350.00"), Instant.now(), List.of());

        when(showService.createShow(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.showId").value(1))
                .andExpect(jsonPath("$.movieName").value("Dune Part Two"))
                .andExpect(jsonPath("$.totalSeats").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/shows - Should return 400 Bad Request on invalid payload")
    void shouldReturn400OnInvalidShowCreation() throws Exception {
        CreateShowRequest invalidRequest = new CreateShowRequest("", null, 0, new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/v1/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    @DisplayName("GET /api/v1/shows/{showId}/available-seats - Should return available seats")
    void shouldReturnAvailableSeats() throws Exception {
        SeatResponse seat1 = new SeatResponse(1L, 1L, "A1", false);
        when(showService.getAvailableSeats(1L)).thenReturn(List.of(seat1));

        mockMvc.perform(get("/api/v1/shows/1/available-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatId").value(1))
                .andExpect(jsonPath("$[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$[0].isBooked").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/shows/{showId} - Should return 404 when show not found")
    void shouldReturn404WhenShowNotFound() throws Exception {
        when(showService.getShowById(999L)).thenThrow(new ShowNotFoundException(999L));

        mockMvc.perform(get("/api/v1/shows/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("SHOW_NOT_FOUND"));
    }
}
