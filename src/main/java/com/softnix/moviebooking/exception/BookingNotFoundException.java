package com.softnix.moviebooking.exception;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(Long bookingId) {
        super("Booking with ID " + bookingId + " was not found");
    }

    public BookingNotFoundException(String message) {
        super(message);
    }
}
