package com.softnix.moviebooking.exception;

public class ShowNotFoundException extends RuntimeException {
    public ShowNotFoundException(Long showId) {
        super("Show with ID " + showId + " was not found");
    }

    public ShowNotFoundException(String message) {
        super(message);
    }
}
