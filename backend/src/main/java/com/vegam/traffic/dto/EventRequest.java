package com.vegam.traffic.dto;

import com.vegam.traffic.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for POST /api/events. {@code factor} (slowdown multiplier, e.g.
 * 1.5 = 50% slower) is required for CONGESTION events and must be absent
 * for CLOSURE/REOPEN events.
 */
public record EventRequest(
        @NotNull(message = "Event type must not be null (CLOSURE, REOPEN or CONGESTION)") EventType type,
        @NotBlank(message = "Source junction must not be blank") String from,
        @NotBlank(message = "Destination junction must not be blank") String to,
        Double factor) {
}
