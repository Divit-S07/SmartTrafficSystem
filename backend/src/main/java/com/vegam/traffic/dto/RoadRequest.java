package com.vegam.traffic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload for POST /api/roads. A two-way road ({@code oneWay=false}) is
 * stored as two directed segments (one per direction), exactly like the
 * paired directed edges of the original Java code.
 */
public record RoadRequest(
        @NotBlank(message = "Source junction must not be blank") String from,
        @NotBlank(message = "Destination junction must not be blank") String to,
        @NotNull(message = "Travel time must not be null")
        @Positive(message = "Travel time must be a positive number of minutes") Double travelTime,
        @NotNull(message = "Direction type must be given (oneWay=true for one-way, false for two-way)") Boolean oneWay) {
}
