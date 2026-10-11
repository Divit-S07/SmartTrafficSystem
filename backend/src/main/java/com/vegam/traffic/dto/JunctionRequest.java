package com.vegam.traffic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for POST /api/junctions. The id is the unique junction identifier
 * (e.g. "J1"); x/y are the map coordinates used by the frontend.
 */
public record JunctionRequest(
        @NotBlank(message = "Junction id must not be blank") String id,
        @NotNull(message = "Junction x coordinate must not be null") Double x,
        @NotNull(message = "Junction y coordinate must not be null") Double y) {
}
