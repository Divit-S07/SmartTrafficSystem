/**
 * Domain model classes persisted in MongoDB, ported from the original
 * single-file SmartTrafficSystem.java and shaped to match the TypeScript
 * frontend interfaces exactly:
 * <ul>
 *   <li>{@link com.vegam.traffic.model.Junction} — collection {@code junctions}</li>
 *   <li>{@link com.vegam.traffic.model.RoadSegment} — collection {@code road_segments}</li>
 *   <li>{@link com.vegam.traffic.model.EmergencyVehicle} — collection {@code emergency_vehicles}</li>
 *   <li>{@link com.vegam.traffic.model.TrafficEvent} — collection {@code traffic_events}</li>
 * </ul>
 * plus the embedded value type {@link com.vegam.traffic.model.Route} and the
 * enums VehicleType, VehicleStatus, RouteStatus, EventType, EventStatus.
 * Jakarta Validation annotations are declared on every field; enforcement
 * happens in the service layer (next step).
 */
package com.vegam.traffic.model;
