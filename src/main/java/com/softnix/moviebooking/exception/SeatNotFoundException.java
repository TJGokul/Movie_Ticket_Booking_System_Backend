package com.softnix.moviebooking.exception;

public class SeatNotFoundException extends RuntimeException {
    public SeatNotFoundException(Long seatId, Long showId) {
        super("Seat with ID " + seatId + " was not found for show " + showId);
    }

    public SeatNotFoundException(Long seatId) {
        super("Seat with ID " + seatId + " was not found");
    }

    public SeatNotFoundException(String message) {
        super(message);
    }
}
