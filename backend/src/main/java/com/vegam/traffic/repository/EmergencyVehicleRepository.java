package com.vegam.traffic.repository;

import com.vegam.traffic.model.EmergencyVehicle;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Persistence for {@code emergency_vehicles}. Vehicle ids are unique because
 * the business id (e.g. "AMB1") is the document {@code _id}.
 */
public interface EmergencyVehicleRepository extends MongoRepository<EmergencyVehicle, String> {
}
