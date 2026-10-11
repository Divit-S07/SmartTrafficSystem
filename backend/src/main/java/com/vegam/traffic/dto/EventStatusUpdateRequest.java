package com.vegam.traffic.dto;

import com.vegam.traffic.model.EventStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for PATCH /api/events/{id}/status. Setting PROCESSED applies the
 * event's effect to the road (close/reopen/congestion), exactly like the Java
 * {@code processNextTrafficEvent()}. Setting back to PENDING re-queues the
 * event but does NOT revert road changes (the Java program never reverts
 * applied events either).
 */
public record EventStatusUpdateRequest(
        @NotNull(message = "Status must not be null (PENDING or PROCESSED)") EventStatus status) {
}
