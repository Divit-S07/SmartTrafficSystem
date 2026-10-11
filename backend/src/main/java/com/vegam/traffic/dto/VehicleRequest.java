package com.vegam.traffic.dto;

import com.vegam.traffic.model.Severity;
import com.vegam.traffic.model.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload for POST /api/vehicles — emergency vehicle registration. */
public record VehicleRequest(
        @NotBlank(message = "Vehicle id must not be blank") String id,
        @NotNull(message = "Vehicle type must not be null") VehicleType type,
        @NotBlank(message = "Starting junction must not be blank") String junction,
        Severity priority) {
}
