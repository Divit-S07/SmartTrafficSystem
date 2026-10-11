package com.vegam.traffic.service;

import com.vegam.traffic.dto.RoadRequest;
import com.vegam.traffic.dto.RoadResponse;
import com.vegam.traffic.dto.RoadStatusUpdateRequest;
import com.vegam.traffic.dto.RoadUpdateRequest;
import com.vegam.traffic.exception.ConflictException;
import com.vegam.traffic.exception.NotFoundException;
import com.vegam.traffic.model.RoadSegment;
import com.vegam.traffic.repository.JunctionRepository;
import com.vegam.traffic.repository.RoadSegmentRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Road use cases.
 *
 * <p>Model: a road resource is one <em>directed</em> segment; a two-way road
 * is stored as two segments (one per direction) that are always created,
 * updated, re-statused and deleted together — the same paired-edge
 * representation as the original Java {@code RoadNetwork}.
 *
 * <p>Duplicate detection is defined uniformly on directed edges: a new road
 * is a duplicate if <b>any of the directed edges it would create already
 * exists</b>. So a one-way A&rarr;B conflicts with an existing A&rarr;B
 * (whatever its stored direction), and a two-way A&harr;B conflicts if either
 * A&rarr;B or B&rarr;A exists.
 */
@Service
public class RoadService {

    private final RoadSegmentRepository roadRepository;
    private final JunctionRepository junctionRepository;

    public RoadService(RoadSegmentRepository roadRepository, JunctionRepository junctionRepository) {
        this.roadRepository = roadRepository;
        this.junctionRepository = junctionRepository;
    }

    public List<RoadResponse> listRoads() {
        return roadRepository.findAll(Sort.by(Sort.Direction.ASC, "from", "to")).stream()
                .map(RoadResponse::from)
                .collect(Collectors.toList());
    }

    public RoadResponse createRoad(RoadRequest request) {
        String from = request.from();
        String to = request.to();
        if (from.equals(to)) {
            throw new IllegalArgumentException(
                    "Source and destination junctions must be different (got '" + from + "' for both)");
        }
        requireJunction(from);
        requireJunction(to);

        boolean oneWay = request.oneWay();
        List<String> edges = oneWay
                ? List.of(edge(from, to))
                : List.of(edge(from, to), edge(to, from));
        // Exact pair lookup: findByFromOrTo(from, to) means "from=from OR to=to",
        // which would miss the reverse edge of a two-way pair in some orders.
        Set<String> existing = new HashSet<>();
        if (roadRepository.existsByFromAndTo(from, to)) {
            existing.add(edge(from, to));
        }
        if (roadRepository.existsByFromAndTo(to, from)) {
            existing.add(edge(to, from));
        }
        List<String> duplicates = edges.stream().filter(existing::contains).collect(Collectors.toList());
        if (!duplicates.isEmpty()) {
            throw new ConflictException("Road(s) already exist: " + String.join(", ", duplicates)
                    + (oneWay ? "" : " (duplicate detected for the two-way connection " + from + " <-> " + to + ")"));
        }

        RoadSegment primary = saveSegment(from, to, request.travelTime(), oneWay, false);
        if (!oneWay) {
            saveSegment(to, from, request.travelTime(), false, false);
        }
        return RoadResponse.from(primary);
    }

    public RoadResponse updateRoad(String id, RoadUpdateRequest request) {
        RoadSegment primary = findOr404(id);
        RoadSegment pair = findPair(primary);

        if (request.oneWay()) {
            // Becoming/staying one-way: the reverse direction is removed if it existed.
            if (pair != null) {
                roadRepository.delete(pair);
            }
        } else if (pair != null) {
            // Staying two-way: the reverse segment follows the new attributes.
            pair.setBaseTravelTime(request.travelTime());
            pair.setCurrentTravelTime(request.travelTime());
            pair.setClosed(request.closed());
            roadRepository.save(pair);
        } else {
            // Becoming two-way: the reverse segment must not exist independently.
            if (roadRepository.existsByFromAndTo(primary.getTo(), primary.getFrom())) {
                throw new ConflictException("Cannot make road " + primary.getFrom() + "->" + primary.getTo()
                        + " two-way: the reverse road " + primary.getTo() + "->" + primary.getFrom()
                        + " already exists as a separate one-way road. Delete it first.");
            }
            saveSegment(primary.getTo(), primary.getFrom(), request.travelTime(), false, request.closed());
        }

        primary.setBaseTravelTime(request.travelTime());
        primary.setCurrentTravelTime(request.travelTime());
        primary.setOneWay(request.oneWay());
        primary.setClosed(request.closed());
        return RoadResponse.from(roadRepository.save(primary));
    }

    public RoadResponse updateStatus(String id, RoadStatusUpdateRequest request) {
        RoadSegment primary = findOr404(id);
        RoadSegment pair = findPair(primary);
        boolean closed = request.closed();

        applyStatus(primary, closed);
        if (pair != null) {
            applyStatus(pair, closed);
            roadRepository.save(pair);
        }
        return RoadResponse.from(roadRepository.save(primary));
    }

    public void deleteRoad(String id) {
        RoadSegment primary = findOr404(id);
        RoadSegment pair = findPair(primary);
        if (pair != null) {
            roadRepository.delete(pair);
        }
        roadRepository.delete(primary);
    }

    /** Closing/reopening follows the original Java semantics: reopening restores the base travel time. */
    private void applyStatus(RoadSegment segment, boolean closed) {
        segment.setClosed(closed);
        if (!closed) {
            segment.setCurrentTravelTime(segment.getBaseTravelTime());
        }
    }

    /** The reverse segment of a two-way road, if one exists. One-way segments have no pair. */
    private RoadSegment findPair(RoadSegment segment) {
        if (segment.isOneWay()) {
            return null;
        }
        return roadRepository.findByFromAndTo(segment.getTo(), segment.getFrom())
                .filter(reverse -> !reverse.isOneWay())
                .orElse(null);
    }

    private RoadSegment findOr404(String id) {
        return roadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Road '" + id + "' not found"));
    }

    private void requireJunction(String junctionId) {
        if (!junctionRepository.existsById(junctionId)) {
            throw new IllegalArgumentException("Unknown junction '" + junctionId
                    + "' — create it first via POST /api/junctions");
        }
    }

    private RoadSegment saveSegment(String from, String to, double travelTime, boolean oneWay, boolean closed) {
        try {
            return roadRepository.save(new RoadSegment(from, to, travelTime, travelTime, closed, oneWay));
        } catch (DuplicateKeyException ex) {
            // Unique compound index raced with a concurrent insert.
            throw new ConflictException("Road(s) already exist: " + edge(from, to));
        }
    }

    private static String edge(String from, String to) {
        return from + "->" + to;
    }
}
