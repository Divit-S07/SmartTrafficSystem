package com.vegam.traffic.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for PATCH /api/roads/{id}/status — closes (closed=true) or
 * reopens (closed=false) a road. Reopening restores the current travel time
 * to the base travel time, matching {@code reopen()} in the original Java code.
 */
public record RoadStatusUpdateRequest(
        @NotNull(message = "Status must be given (closed=true for closed, false for open)") Boolean closed) {
}
