package com.vegam.traffic.dto;

import com.vegam.traffic.model.Severity;
import jakarta.validation.constraints.NotNull;

/** Payload for PUT /api/vehicles/{id}/priority. */
public record VehiclePriorityUpdateRequest(
        @NotNull(message = "Priority must not be null (CRITICAL, HIGH or NORMAL)") Severity priority) {
}
