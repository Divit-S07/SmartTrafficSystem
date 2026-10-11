/**
 * Application services. {@link com.vegam.traffic.service.JunctionService},
 * {@link com.vegam.traffic.service.RoadService} and
 * {@link com.vegam.traffic.service.RoutingService} are implemented (CRUD,
 * unique-id/unique-edge enforcement, Jakarta Validation enforcement,
 * road-reference-safe junction deletion, close/reopen of roads, and route
 * calculation via the Dijkstra ported from SmartTrafficSystem.java).
 * VehicleService, EventService and SeedService — including the emergency
 * priority and FIFO traffic-event queue logic — follow in the next steps.
 */
package com.vegam.traffic.service;
