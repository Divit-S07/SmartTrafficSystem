package com.vegam.traffic.service;

import com.vegam.traffic.dto.JunctionPositionUpdateRequest;
import com.vegam.traffic.dto.JunctionRequest;
import com.vegam.traffic.dto.JunctionResponse;
import com.vegam.traffic.exception.ConflictException;
import com.vegam.traffic.exception.NotFoundException;
import com.vegam.traffic.model.Junction;
import com.vegam.traffic.model.RoadSegment;
import com.vegam.traffic.repository.JunctionRepository;
import com.vegam.traffic.repository.RoadSegmentRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Junction use cases. The service enforces the model's Jakarta Validation
 * constraints (the repository does not validate on save), guarantees junction
 * id uniqueness, and refuses to delete a junction that roads still reference.
 */
@Service
public class JunctionService {

    private final JunctionRepository junctionRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final Validator validator;

    public JunctionService(JunctionRepository junctionRepository,
                           RoadSegmentRepository roadSegmentRepository,
                           Validator validator) {
        this.junctionRepository = junctionRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.validator = validator;
    }

    public List<JunctionResponse> listJunctions() {
        return junctionRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(JunctionResponse::from)
                .collect(Collectors.toList());
    }

    public JunctionResponse createJunction(JunctionRequest request) {
        Junction junction = new Junction(request.id(), request.x(), request.y());
        validate(junction);

        if (junctionRepository.existsById(junction.getId())) {
            throw new ConflictException("Junction '" + junction.getId() + "' already exists");
        }
        Junction saved;
        try {
            saved = junctionRepository.save(junction);
        } catch (DuplicateKeyException ex) {
            // Unique _id constraint raced with a concurrent insert.
            throw new ConflictException("Junction '" + junction.getId() + "' already exists");
        }
        return JunctionResponse.from(saved);
    }

    public JunctionResponse getJunction(String id) {
        return junctionRepository.findById(id)
                .map(JunctionResponse::from)
                .orElseThrow(() -> new NotFoundException("Junction '" + id + "' not found"));
    }

    /**
     * Persists a dragged map position. Only the layout coordinates change —
     * the junction id and every road referencing it are untouched, so routing
     * is unaffected.
     */
    public JunctionResponse updatePosition(String id, JunctionPositionUpdateRequest request) {
        Junction junction = junctionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Junction '" + id + "' not found"));
        validate(new Junction(id, request.x(), request.y()));
        junction.setX(request.x());
        junction.setY(request.y());
        return JunctionResponse.from(junctionRepository.save(junction));
    }

    public void deleteJunction(String id) {
        Junction junction = junctionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Junction '" + id + "' not found"));

        List<RoadSegment> referencing = roadSegmentRepository.findByFromOrTo(id, id);
        if (!referencing.isEmpty()) {
            String edges = referencing.stream()
                    .map(r -> r.getFrom() + "->" + r.getTo())
                    .collect(Collectors.joining(", "));
            throw new ConflictException("Cannot delete junction '" + id
                    + "': it is referenced by road(s) [" + edges
                    + "]. Delete those roads first.");
        }
        junctionRepository.delete(junction);
    }

    /** Applies the model's field constraints; violations become a 400 with clear messages. */
    private void validate(Junction junction) {
        Set<ConstraintViolation<Junction>> violations = validator.validate(junction);
        if (!violations.isEmpty()) {
            String messages = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Validation failed: " + messages);
        }
    }
}
