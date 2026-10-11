package com.vegam.traffic.controller;

import com.vegam.traffic.dto.JunctionPositionUpdateRequest;
import com.vegam.traffic.dto.JunctionRequest;
import com.vegam.traffic.dto.JunctionResponse;
import com.vegam.traffic.service.JunctionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Junction management endpoints.
 *
 * <ul>
 *   <li>GET    /api/junctions        → 200, all junctions (sorted by id)</li>
 *   <li>POST   /api/junctions        → 201 + Location, or 400/409</li>
 *   <li>GET    /api/junctions/{id}   → 200, or 404</li>
 *   <li>PUT    /api/junctions/{id}   → 200 (map position), or 400/404</li>
 *   <li>DELETE /api/junctions/{id}   → 204, 404, or 409 (roads still reference it)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/junctions")
public class JunctionController {

    private final JunctionService junctionService;

    public JunctionController(JunctionService junctionService) {
        this.junctionService = junctionService;
    }

    @GetMapping
    public List<JunctionResponse> listJunctions() {
        return junctionService.listJunctions();
    }

    @PostMapping
    public ResponseEntity<JunctionResponse> createJunction(@Valid @RequestBody JunctionRequest request) {
        JunctionResponse created = junctionService.createJunction(request);
        return ResponseEntity
                .created(URI.create("/api/junctions/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    public JunctionResponse getJunction(@PathVariable String id) {
        return junctionService.getJunction(id);
    }

    @PutMapping("/{id}")
    public JunctionResponse updateJunction(@PathVariable String id,
                                           @Valid @RequestBody JunctionPositionUpdateRequest request) {
        return junctionService.updatePosition(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJunction(@PathVariable String id) {
        junctionService.deleteJunction(id);
    }
}
