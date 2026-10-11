package com.vegam.traffic.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Map position update for a junction (dragged on the React Flow canvas).
 *
 * <p>Coordinates are normalised canvas units; only the layout changes, never
 * the network topology, so no algorithm state is affected.
 */
public record JunctionPositionUpdateRequest(
        @NotNull(message = "Junction x coordinate must not be null") Double x,
        @NotNull(message = "Junction y coordinate must not be null") Double y) {
}
