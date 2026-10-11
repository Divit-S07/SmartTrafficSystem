package com.vegam.traffic.dto;

import com.vegam.traffic.model.Junction;

/**
 * Junction payload returned by the API. Field names match the TypeScript
 * frontend {@code Junction} interface exactly: { id, x, y }.
 */
public record JunctionResponse(String id, Double x, Double y) {

    public static JunctionResponse from(Junction junction) {
        return new JunctionResponse(junction.getId(), junction.getX(), junction.getY());
    }
}
