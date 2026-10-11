package com.vegam.traffic.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload for POST /api/routes/calculate. */
public record RouteRequest(
        @NotBlank(message = "Source junction must not be blank") String source,
        @NotBlank(message = "Destination junction must not be blank") String destination) {
}
