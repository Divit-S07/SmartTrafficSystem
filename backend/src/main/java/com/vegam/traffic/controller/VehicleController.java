package com.vegam.traffic.controller;

import com.vegam.traffic.dto.VehicleJunctionUpdateRequest;
import com.vegam.traffic.dto.VehiclePriorityUpdateRequest;
import com.vegam.traffic.dto.VehicleRequest;
import com.vegam.traffic.dto.VehicleResponse;
import com.vegam.traffic.service.VehicleService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Emergency vehicle endpoints.
 *
 * <ul>
 *   <li>GET  /api/vehicles            → 200, vehicles in emergency-priority order (CRITICAL first)</li>
 *   <li>POST /api/vehicles            → 201 + Location, or 400/404/409</li>
 *   <li>GET  /api/vehicles/{id}       → 200, or 404</li>
 *   <li>PUT  /api/vehicles/{id}/junction  → 200 (must be adjacent via an open road), 400/404/409</li>
 *   <li>PUT  /api/vehicles/{id}/priority → 200, 400/404</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleResponse> listVehicles() {
        return vehicleService.listVehicles();
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> registerVehicle(@Valid @RequestBody VehicleRequest request) {
        VehicleResponse created = vehicleService.registerVehicle(request);
        return ResponseEntity
                .created(URI.create("/api/vehicles/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    public VehicleResponse getVehicle(@PathVariable String id) {
        return vehicleService.getVehicle(id);
    }

    @PutMapping("/{id}/junction")
    public VehicleResponse updateJunction(@PathVariable String id,
                                          @Valid @RequestBody VehicleJunctionUpdateRequest request) {
        return vehicleService.updateJunction(id, request);
    }

    @PutMapping("/{id}/priority")
    public VehicleResponse updatePriority(@PathVariable String id,
                                          @Valid @RequestBody VehiclePriorityUpdateRequest request) {
        return vehicleService.updatePriority(id, request);
    }
}
