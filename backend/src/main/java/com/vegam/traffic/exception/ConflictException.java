package com.vegam.traffic.exception;

/**
 * Thrown when a request conflicts with the current state, e.g. creating a
 * junction whose id already exists, or deleting a junction that roads still
 * reference. Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
