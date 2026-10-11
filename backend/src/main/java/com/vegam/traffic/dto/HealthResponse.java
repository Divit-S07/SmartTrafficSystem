package com.vegam.traffic.dto;

/**
 * Response for GET /api/health. Further DTOs (junction, road, vehicle,
 * request, event payloads) are added when the real endpoints are implemented.
 */
public record HealthResponse(String status, String application) {
}
