package com.softnix.moviebooking.exception;

public class SeatAlreadyBookedException extends RuntimeException {
    public SeatAlreadyBookedException(String seatNumber, Long showId) {
        super("Seat " + seatNumber + " is already booked for show " + showId);
    }

    public SeatAlreadyBookedException(Long seatId, Long showId) {
        super("Seat with ID " + seatId + " is already booked for show " + showId);
    }

    public SeatAlreadyBookedException(String message) {
        super(message);
    }
}
