package com.vegam.traffic.model;

/**
 * Lifecycle status of an emergency vehicle. Values match the TypeScript union
 * and the original Java {@code VehicleStatus} enum.
 */
public enum VehicleStatus {
    IDLE,
    EN_ROUTE,
    ARRIVED,
    UNREACHABLE
}
