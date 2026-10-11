package com.vegam.traffic.dto;

import com.vegam.traffic.model.EventStatus;
import com.vegam.traffic.model.EventType;
import com.vegam.traffic.model.TrafficEvent;

/**
 * Event payload returned by the API. Field names match the TypeScript
 * frontend {@code TrafficEvent} interface.
 */
public record EventResponse(String id, Integer sequence, EventType type, String from, String to,
                            boolean reopening, Double factor, String description, EventStatus status) {

    public static EventResponse from(TrafficEvent event) {
        return new EventResponse(event.getId(), event.getSequence(), event.getType(), event.getFrom(),
                event.getTo(), event.isReopening(), event.getFactor(), event.getDescription(),
                event.getStatus());
    }
}
