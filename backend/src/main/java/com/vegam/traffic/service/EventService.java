package com.vegam.traffic.service;

import com.vegam.traffic.dto.EventRequest;
import com.vegam.traffic.dto.EventResponse;
import com.vegam.traffic.dto.EventStatusUpdateRequest;
import com.vegam.traffic.exception.ConflictException;
import com.vegam.traffic.exception.NotFoundException;
import com.vegam.traffic.model.EventStatus;
import com.vegam.traffic.model.RoadSegment;
import com.vegam.traffic.model.TrafficEvent;
import com.vegam.traffic.repository.JunctionRepository;
import com.vegam.traffic.repository.RoadSegmentRepository;
import com.vegam.traffic.repository.TrafficEventRepository;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Traffic event use cases. Events receive a strictly increasing {@code
 * sequence} (unique index) — the FIFO order of the original Java
 * {@code eventQueue}. Setting an event to PROCESSED applies its effect to the
 * directed road segment with the exact semantics of the Java
 * {@code TrafficEvent.applyToNetwork()}: CLOSE sets closed, REOPEN clears it
 * and restores the base travel time, CONGESTION sets current travel time to
 * base &times; factor (idempotent, never compounded).
 */
@Service
public class EventService {

    private final TrafficEventRepository eventRepository;
    private final JunctionRepository junctionRepository;
    private final RoadSegmentRepository roadRepository;

    public EventService(TrafficEventRepository eventRepository,
                        JunctionRepository junctionRepository,
                        RoadSegmentRepository roadRepository) {
        this.eventRepository = eventRepository;
        this.junctionRepository = junctionRepository;
        this.roadRepository = roadRepository;
    }

    public List<EventResponse> listEvents() {
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "sequence")).stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
    }

    public EventResponse createEvent(EventRequest request) {
        if (request.from().equals(request.to())) {
            throw new IllegalArgumentException(
                    "Source and destination junctions must be different (got '" + request.from() + "' for both)");
        }
        requireJunction(request.from(), "Source");
        requireJunction(request.to(), "Destination");
        validateFactor(request);

        TrafficEvent event = new TrafficEvent();
        event.setSequence(nextSequence());
        event.setType(request.type());
        event.setFrom(request.from());
        event.setTo(request.to());
        event.setReopening(request.type() == com.vegam.traffic.model.EventType.REOPEN);
        event.setFactor(request.type() == com.vegam.traffic.model.EventType.CONGESTION ? request.factor() : null);
        event.setDescription(describe(request));
        event.setStatus(EventStatus.PENDING);
        return EventResponse.from(eventRepository.save(event));
    }

    public EventResponse getEvent(String id) {
        return eventRepository.findById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new NotFoundException("Event '" + id + "' not found"));
    }

    public EventResponse updateStatus(String id, EventStatusUpdateRequest request) {
        TrafficEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event '" + id + "' not found"));

        if (request.status() == EventStatus.PROCESSED) {
            applyToRoad(event);
        }
        // PENDING: re-queue only. The Java program never reverts applied road
        // changes either, so un-processing does not undo the road effect.
        event.setStatus(request.status());
        return EventResponse.from(eventRepository.save(event));
    }

    /** Exact port of the Java {@code applyToNetwork()} semantics per event type. */
    private void applyToRoad(TrafficEvent event) {
        RoadSegment segment = roadRepository.findByFromAndTo(event.getFrom(), event.getTo())
                .orElseThrow(() -> new ConflictException("No road " + event.getFrom() + "->" + event.getTo()
                        + " to apply " + event.getType() + " to (road may have been deleted)"));
        switch (event.getType()) {
            case CLOSURE -> segment.setClosed(true);
            case REOPEN -> {
                segment.setClosed(false);
                segment.setCurrentTravelTime(segment.getBaseTravelTime());
            }
            case CONGESTION -> segment.setCurrentTravelTime(
                    segment.getBaseTravelTime() * Math.max(1.0, event.getFactor()));
        }
        roadRepository.save(segment);
    }

    private int nextSequence() {
        return eventRepository.findTopByOrderBySequenceDesc()
                .map(TrafficEvent::getSequence)
                .orElse(0) + 1;
    }

    private void validateFactor(EventRequest request) {
        if (request.type() == com.vegam.traffic.model.EventType.CONGESTION) {
            if (request.factor() == null) {
                throw new IllegalArgumentException(
                        "CONGESTION events require a slowdown factor (e.g. 1.5 = 50% slower)");
            }
            if (request.factor() < 1.0) {
                throw new IllegalArgumentException(
                        "Slowdown factor must be a finite number >= 1.0 (got " + request.factor() + ")");
            }
        } else if (request.factor() != null) {
            throw new IllegalArgumentException("Slowdown factor only applies to CONGESTION events");
        }
    }

    /** Description format of the original Java event classes. */
    private String describe(EventRequest request) {
        return switch (request.type()) {
            case CLOSURE -> "CLOSE " + request.from() + "->" + request.to();
            case REOPEN -> "REOPEN " + request.from() + "->" + request.to();
            case CONGESTION -> String.format(Locale.ROOT, "CONGESTION %s->%s x%s",
                    request.from(), request.to(), request.factor());
        };
    }

    private void requireJunction(String junctionId, String role) {
        if (!junctionRepository.existsById(junctionId)) {
            throw new NotFoundException("Unknown junction '" + junctionId + "' as " + role.toLowerCase()
                    + " junction — create it first via POST /api/junctions");
        }
    }
}
