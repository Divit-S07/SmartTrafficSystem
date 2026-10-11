package com.vegam.traffic.dto;

import java.util.List;

/**
 * Result of a route calculation. {@code status} is "VALID" or "UNREACHABLE"
 * (the TypeScript frontend's RouteStatus union). For an UNREACHABLE result
 * {@code path} is empty, {@code totalTravelTime} is null, and {@code message}
 * explains why no route exists.
 */
public record RouteResponse(List<String> path, Double totalTravelTime, String status,
                            String requestedFrom, String requestedTo, String message) {
}
