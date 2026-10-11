package com.vegam.traffic.controller;

import com.vegam.traffic.dto.RoadRequest;
import com.vegam.traffic.dto.RoadResponse;
import com.vegam.traffic.dto.RoadStatusUpdateRequest;
import com.vegam.traffic.dto.RoadUpdateRequest;
import com.vegam.traffic.service.RoadService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

/**
 * Road management endpoints.
 *
 * <ul>
 *   <li>GET    /api/roads             → 200, all directed road segments</li>
 *   <li>POST   /api/roads             → 201 + Location, or 400/409</li>
 *   <li>PUT    /api/roads/{id}        → 200, 400/404/409</li>
 *   <li>DELETE /api/roads/{id}        → 204, or 404 (removes a two-way pair together)</li>
 *   <li>PATCH  /api/roads/{id}/status → 200, 400/404 — close or reopen</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/roads")
public class RoadController {

    private final RoadService roadService;

    public RoadController(RoadService roadService) {
        this.roadService = roadService;
    }

    @GetMapping
    public List<RoadResponse> listRoads() {
        return roadService.listRoads();
    }

    @PostMapping
    public ResponseEntity<RoadResponse> createRoad(@Valid @RequestBody RoadRequest request) {
        RoadResponse created = roadService.createRoad(request);
        return ResponseEntity
                .created(URI.create("/api/roads/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public RoadResponse updateRoad(@PathVariable String id, @Valid @RequestBody RoadUpdateRequest request) {
        return roadService.updateRoad(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoad(@PathVariable String id) {
        roadService.deleteRoad(id);
    }

    @PatchMapping("/{id}/status")
    public RoadResponse updateStatus(@PathVariable String id,
                                     @Valid @RequestBody RoadStatusUpdateRequest request) {
        return roadService.updateStatus(id, request);
    }
}
