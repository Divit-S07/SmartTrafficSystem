package com.vegam.traffic.controller;

import com.vegam.traffic.dto.EventRequest;
import com.vegam.traffic.dto.EventResponse;
import com.vegam.traffic.dto.EventStatusUpdateRequest;
import com.vegam.traffic.service.EventService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Traffic event endpoints.
 *
 * <ul>
 *   <li>GET   /api/events           → 200, all events in processing (FIFO) order</li>
 *   <li>POST  /api/events           → 201 + Location, or 400/404</li>
 *   <li>GET   /api/events/{id}      → 200, or 404</li>
 *   <li>PATCH /api/events/{id}/status → 200; setting PROCESSED applies the
 *       event's effect to the road (close/reopen/congestion), as in the
 *       original Java processing loop. 400/404/409.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventResponse> listEvents() {
        return eventService.listEvents();
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        EventResponse created = eventService.createEvent(request);
        return ResponseEntity
                .created(URI.create("/api/events/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable String id) {
        return eventService.getEvent(id);
    }

    @PatchMapping("/{id}/status")
    public EventResponse updateStatus(@PathVariable String id,
                                      @Valid @RequestBody EventStatusUpdateRequest request) {
        return eventService.updateStatus(id, request);
    }
}
