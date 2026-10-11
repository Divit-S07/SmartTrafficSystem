package com.vegam.traffic.controller;

import com.vegam.traffic.dto.RouteRequest;
import com.vegam.traffic.dto.RouteResponse;
import com.vegam.traffic.service.RoutingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Route calculation endpoints.
 *
 * <ul>
 *   <li>POST /api/routes/calculate → 200 with the computed route (status
 *       VALID or UNREACHABLE), or 400 when a junction is unknown/blank.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RoutingService routingService;

    public RouteController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/calculate")
    public RouteResponse calculateRoute(@Valid @RequestBody RouteRequest request) {
        return routingService.calculateRoute(request);
    }
}
