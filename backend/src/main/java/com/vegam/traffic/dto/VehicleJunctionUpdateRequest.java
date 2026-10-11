package com.vegam.traffic.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload for PUT /api/vehicles/{id}/junction. */
public record VehicleJunctionUpdateRequest(
        @NotBlank(message = "Junction must not be blank") String junction) {
}
