package com.vegam.traffic.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * An emergency vehicle registered in the system.
 *
 * <p>The business id (e.g. "AMB1") is used directly as the MongoDB {@code _id},
 * so vehicle ids are inherently unique. Field names mirror the TypeScript
 * {@code EmergencyVehicle} interface so the REST payloads will match the
 * existing frontend exactly.
 */
@Document(collection = "emergency_vehicles")
public class EmergencyVehicle {

    @Id
    @NotBlank(message = "Vehicle id must not be blank")
    private String id;

    @NotNull(message = "Vehicle type must not be null")
    private VehicleType type;

    @NotBlank(message = "Vehicle last confirmed junction must not be blank")
    private String lastConfirmedJunction;

    /** Destination junction; null until a routing request has been processed. */
    private String destination;

    @NotNull(message = "Vehicle status must not be null")
    private VehicleStatus status;

    /** Emergency priority; drives the priority ordering of the fleet. */
    @NotNull(message = "Vehicle priority must not be null")
    private Severity priority = Severity.NORMAL;

    /** Currently assigned route; null while the vehicle is IDLE. */
    @Valid
    private Route currentRoute;

    public EmergencyVehicle() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public String getLastConfirmedJunction() {
        return lastConfirmedJunction;
    }

    public void setLastConfirmedJunction(String lastConfirmedJunction) {
        this.lastConfirmedJunction = lastConfirmedJunction;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public Severity getPriority() {
        return priority;
    }

    public void setPriority(Severity priority) {
        this.priority = priority;
    }

    public Route getCurrentRoute() {
        return currentRoute;
    }

    public void setCurrentRoute(Route currentRoute) {
        this.currentRoute = currentRoute;
    }
}
