package com.vegam.traffic.controller;

import com.vegam.traffic.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness/wiring endpoint. Domain controllers (junctions, roads, vehicles,
 * routing requests, traffic events, routes, activity) are added in the next step.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "smart-traffic-backend");
    }
}
