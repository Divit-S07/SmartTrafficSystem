package com.vegam.traffic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload for PUT /api/roads/{id} — a full replacement of the road's mutable
 * attributes. The endpoints (from/to) are the resource identity and cannot
 * change; changing a road's endpoints means deleting it and creating a new one.
 */
public record RoadUpdateRequest(
        @NotNull(message = "Travel time must not be null")
        @Positive(message = "Travel time must be a positive number of minutes") Double travelTime,
        @NotNull(message = "Direction type must be given (oneWay=true for one-way, false for two-way)") Boolean oneWay,
        @NotNull(message = "Status must be given (closed=true for closed, false for open)") Boolean closed) {
}
