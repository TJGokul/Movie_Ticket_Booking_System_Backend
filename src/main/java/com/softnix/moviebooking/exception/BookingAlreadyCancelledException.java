package com.softnix.moviebooking.exception;

public class BookingAlreadyCancelledException extends RuntimeException {
    public BookingAlreadyCancelledException(Long bookingId) {
        super("Booking with ID " + bookingId + " has already been cancelled");
    }

    public BookingAlreadyCancelledException(String message) {
        super(message);
    }
}
