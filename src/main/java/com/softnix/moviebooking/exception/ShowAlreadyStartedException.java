package com.softnix.moviebooking.exception;

public class ShowAlreadyStartedException extends RuntimeException {
    public ShowAlreadyStartedException(String message) {
        super(message);
    }

    public ShowAlreadyStartedException(Long showId, String action) {
        super("Cannot " + action + " because show with ID " + showId + " has already started or passed");
    }
}
