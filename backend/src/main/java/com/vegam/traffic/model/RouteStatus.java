package com.vegam.traffic.model;

/**
 * Outcome of a route computation. Matches the TypeScript union and the
 * original Java {@code RouteStatus} enum.
 */
public enum RouteStatus {
    VALID,
    UNREACHABLE
}
