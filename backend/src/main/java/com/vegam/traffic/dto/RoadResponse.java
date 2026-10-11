package com.vegam.traffic.dto;

import com.vegam.traffic.model.RoadSegment;

/**
 * Road payload returned by the API. Field names match the TypeScript
 * frontend {@code RoadSegment} interface ({from, to, baseTravelTime,
 * currentTravelTime, oneWay, closed}) plus the resource {@code id}.
 * A two-way road appears as two entries that point at each other.
 */
public record RoadResponse(String id, String from, String to, Double baseTravelTime,
                           Double currentTravelTime, boolean oneWay, boolean closed) {

    public static RoadResponse from(RoadSegment segment) {
        return new RoadResponse(segment.getId(), segment.getFrom(), segment.getTo(),
                segment.getBaseTravelTime(), segment.getCurrentTravelTime(),
                segment.isOneWay(), segment.isClosed());
    }
}
