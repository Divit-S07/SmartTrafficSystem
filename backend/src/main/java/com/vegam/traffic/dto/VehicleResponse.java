package com.vegam.traffic.dto;

import com.vegam.traffic.model.EmergencyVehicle;
import com.vegam.traffic.model.Severity;
import com.vegam.traffic.model.VehicleStatus;
import com.vegam.traffic.model.VehicleType;

/**
 * Vehicle payload returned by the API. Field names match the TypeScript
 * frontend {@code EmergencyVehicle} interface plus {@code priority}.
 */
public record VehicleResponse(String id, VehicleType type, String lastConfirmedJunction,
                              String destination, VehicleStatus status, Severity priority) {

    public static VehicleResponse from(EmergencyVehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getType(), vehicle.getLastConfirmedJunction(),
                vehicle.getDestination(), vehicle.getStatus(), vehicle.getPriority());
    }
}
