package com.vegam.traffic.service;

import com.vegam.traffic.dto.VehicleJunctionUpdateRequest;
import com.vegam.traffic.dto.VehiclePriorityUpdateRequest;
import com.vegam.traffic.dto.VehicleRequest;
import com.vegam.traffic.dto.VehicleResponse;
import com.vegam.traffic.exception.ConflictException;
import com.vegam.traffic.exception.NotFoundException;
import com.vegam.traffic.model.EmergencyVehicle;
import com.vegam.traffic.model.RoadSegment;
import com.vegam.traffic.model.Severity;
import com.vegam.traffic.model.VehicleStatus;
import com.vegam.traffic.repository.JunctionRepository;
import com.vegam.traffic.repository.RoadSegmentRepository;
import com.vegam.traffic.repository.EmergencyVehicleRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Emergency vehicle use cases.
 *
 * <p>Listing order reuses the original Java priority ordering
 * ({@code RoutingRequest.PRIORITY_ORDER}): higher severity rank first, ties
 * broken by id. Junction updates preserve the encapsulation rules of the Java
 * {@code EmergencyVehicle.updatePosition()}: a vehicle may only move to a
 * junction directly reachable through an open road.
 */
@Service
public class VehicleService {

    /** Same ordering intent as the Java priority queue: severity rank desc, then stable tie-break. */
    static final Comparator<EmergencyVehicle> PRIORITY_ORDER =
            Comparator.comparingInt((EmergencyVehicle v) -> v.getPriority().getRank()).reversed()
                    .thenComparing(EmergencyVehicle::getId);

    private final EmergencyVehicleRepository vehicleRepository;
    private final JunctionRepository junctionRepository;
    private final RoadSegmentRepository roadRepository;

    public VehicleService(EmergencyVehicleRepository vehicleRepository,
                          JunctionRepository junctionRepository,
                          RoadSegmentRepository roadRepository) {
        this.vehicleRepository = vehicleRepository;
        this.junctionRepository = junctionRepository;
        this.roadRepository = roadRepository;
    }

    public List<VehicleResponse> listVehicles() {
        // Sorted in memory: the priority rank lives inside the enum, not in a
        // Mongo-sortable field, and the fleet is small by design.
        return vehicleRepository.findAll().stream()
                .sorted(PRIORITY_ORDER)
                .map(VehicleResponse::from)
                .collect(Collectors.toList());
    }

    public VehicleResponse registerVehicle(VehicleRequest request) {
        requireJunction(request.junction(), "Starting");

        EmergencyVehicle vehicle = new EmergencyVehicle();
        vehicle.setId(request.id());
        vehicle.setType(request.type());
        vehicle.setLastConfirmedJunction(request.junction());
        vehicle.setStatus(VehicleStatus.IDLE);
        vehicle.setPriority(request.priority() != null ? request.priority() : Severity.NORMAL);

        if (vehicleRepository.existsById(vehicle.getId())) {
            throw new ConflictException("Vehicle '" + vehicle.getId() + "' is already registered");
        }
        EmergencyVehicle saved;
        try {
            saved = vehicleRepository.save(vehicle);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("Vehicle '" + vehicle.getId() + "' is already registered");
        }
        return VehicleResponse.from(saved);
    }

    public VehicleResponse getVehicle(String id) {
        return vehicleRepository.findById(id)
                .map(VehicleResponse::from)
                .orElseThrow(() -> new NotFoundException("Vehicle '" + id + "' not found"));
    }

    public VehicleResponse updateJunction(String id, VehicleJunctionUpdateRequest request) {
        EmergencyVehicle vehicle = findOr404(id);
        String target = request.junction();
        requireJunction(target, "Target");

        String current = vehicle.getLastConfirmedJunction();
        if (!current.equals(target)) {
            // Java updatePosition() rules: must be adjacent and the road must be open.
            RoadSegment segment = roadRepository.findByFromAndTo(current, target)
                    .orElseThrow(() -> new ConflictException(target + " is not directly reachable from "
                            + current + " (no open road in that direction)"));
            if (segment.isClosed()) {
                throw new ConflictException("Road " + current + " -> " + target
                        + " is closed; vehicle cannot use it");
            }
            vehicle.setLastConfirmedJunction(target);
        }
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    public VehicleResponse updatePriority(String id, VehiclePriorityUpdateRequest request) {
        EmergencyVehicle vehicle = findOr404(id);
        vehicle.setPriority(request.priority());
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    private EmergencyVehicle findOr404(String id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle '" + id + "' not found"));
    }

    private void requireJunction(String junctionId, String role) {
        if (!junctionRepository.existsById(junctionId)) {
            throw new NotFoundException("Unknown junction '" + junctionId + "' as "
                    + role.toLowerCase() + " junction — create it first via POST /api/junctions");
        }
    }
}
