package com.vegam.traffic.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * A computed route, embedded inside {@link EmergencyVehicle} documents.
 * Mirrors the {@code Route} interface of the TypeScript frontend: an
 * UNREACHABLE route has an empty path and an infinite travel time, exactly as
 * {@code Route.unreachable()} behaves in the original Java code.
 */
public class Route {

    @NotNull(message = "Route path must not be null (empty list means UNREACHABLE)")
    private List<String> path = new ArrayList<>();

    @NotNull(message = "Route total travel time must not be null")
    private Double totalTravelTime;

    @NotNull(message = "Route status must not be null")
    private RouteStatus status;

    @NotBlank(message = "Route requestedFrom must not be blank")
    private String requestedFrom;

    @NotBlank(message = "Route requestedTo must not be blank")
    private String requestedTo;

    public Route() {
    }

    public Route(List<String> path, Double totalTravelTime, RouteStatus status,
                 String requestedFrom, String requestedTo) {
        this.path = path;
        this.totalTravelTime = totalTravelTime;
        this.status = status;
        this.requestedFrom = requestedFrom;
        this.requestedTo = requestedTo;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public Double getTotalTravelTime() {
        return totalTravelTime;
    }

    public void setTotalTravelTime(Double totalTravelTime) {
        this.totalTravelTime = totalTravelTime;
    }

    public RouteStatus getStatus() {
        return status;
    }

    public void setStatus(RouteStatus status) {
        this.status = status;
    }

    public String getRequestedFrom() {
        return requestedFrom;
    }

    public void setRequestedFrom(String requestedFrom) {
        this.requestedFrom = requestedFrom;
    }

    public String getRequestedTo() {
        return requestedTo;
    }

    public void setRequestedTo(String requestedTo) {
        this.requestedTo = requestedTo;
    }
}
