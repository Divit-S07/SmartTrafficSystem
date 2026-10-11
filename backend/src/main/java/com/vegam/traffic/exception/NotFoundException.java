package com.vegam.traffic.exception;

/**
 * Thrown when a referenced junction, vehicle, request or event does not exist.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
