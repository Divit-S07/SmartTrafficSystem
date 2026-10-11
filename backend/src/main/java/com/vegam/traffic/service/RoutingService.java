package com.vegam.traffic.service;

import com.vegam.traffic.dto.RouteRequest;
import com.vegam.traffic.dto.RouteResponse;
import com.vegam.traffic.exception.NotFoundException;
import com.vegam.traffic.model.RoadSegment;
import com.vegam.traffic.repository.JunctionRepository;
import com.vegam.traffic.repository.RoadSegmentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Route calculation over the persisted road network.
 *
 * <p>The algorithm is a faithful port of {@code RoadNetwork.shortestPath()}
 * from the original {@code SmartTrafficSystem.java} (hand-written Dijkstra,
 * positive weights, lazy stale-entry deletion in the min-heap, early exit at
 * the destination) — it is deliberately NOT replaced by a library. Semantics
 * preserved from the original:
 * <ul>
 *   <li>each <em>stored directed segment</em> is an edge, so one-way roads
 *       permit travel only in the configured direction and two-way roads
 *       permit both (they are stored as paired directed edges);</li>
 *   <li>closed segments are treated as having infinite weight and are never
 *       selected;</li>
 *   <li>the edge weight is the segment's {@code currentTravelTime}, so
 *       congestion adjustments would be reflected automatically;</li>
 *   <li>a route always starts at the requested source junction; identical
 *       source and destination yield a zero-length valid route.</li>
 * </ul>
 * The emergency-priority (severity) queue and the FIFO traffic-event queue of
 * the original program are untouched in {@code SmartTrafficSystem.java} and
 * will drive this service when vehicles/events are wired in a later step.
 */
@Service
public class RoutingService {

    private final JunctionRepository junctionRepository;
    private final RoadSegmentRepository roadRepository;

    public RoutingService(JunctionRepository junctionRepository, RoadSegmentRepository roadRepository) {
        this.junctionRepository = junctionRepository;
        this.roadRepository = roadRepository;
    }

    public RouteResponse calculateRoute(RouteRequest request) {
        String source = request.source();
        String destination = request.destination();
        requireJunction(source, "Source");
        requireJunction(destination, "Destination");

        // Adjacency over OPEN segments only (closed = infinite weight, per the original).
        Map<String, List<SegmentEdge>> adjacency = new HashMap<>();
        for (RoadSegment segment : roadRepository.findAll()) {
            if (segment.isClosed()) {
                continue;
            }
            adjacency.computeIfAbsent(segment.getFrom(), key -> new ArrayList<>())
                    .add(new SegmentEdge(segment.getTo(), segment.getCurrentTravelTime()));
        }

        // ---- Dijkstra, ported from SmartTrafficSystem.java (RoadNetwork.shortestPath) ----
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> settled = new HashSet<>();
        dist.put(source, 0.0);

        PriorityQueue<HeapEntry> minHeap = new PriorityQueue<>(Comparator.comparingDouble(HeapEntry::distance));
        minHeap.offer(new HeapEntry(source, 0.0));

        while (!minHeap.isEmpty()) {
            HeapEntry entry = minHeap.poll();
            String u = entry.node();
            // Stale heap entry (lazy deletion) / duplicate entry for a settled junction.
            if (entry.distance() > dist.getOrDefault(u, Double.POSITIVE_INFINITY)) {
                continue;
            }
            if (!settled.add(u)) {
                continue;
            }
            if (u.equals(destination)) {
                break;
            }
            for (SegmentEdge edge : adjacency.getOrDefault(u, List.of())) {
                String v = edge.to();
                if (settled.contains(v)) {
                    continue;
                }
                double candidate = dist.get(u) + edge.travelTime();
                if (candidate < dist.getOrDefault(v, Double.POSITIVE_INFINITY)) {
                    dist.put(v, candidate);
                    prev.put(v, u);
                    minHeap.offer(new HeapEntry(v, candidate)); // decrease-key via insert + lazy stale check
                }
            }
        }
        // ---- end of port ----

        if (dist.getOrDefault(destination, Double.POSITIVE_INFINITY) == Double.POSITIVE_INFINITY) {
            return unreachable(source, destination);
        }

        LinkedList<String> path = new LinkedList<>();
        String current = destination;
        while (current != null) {
            path.addFirst(current);
            if (current.equals(source)) {
                break;
            }
            current = prev.get(current);
        }
        if (path.isEmpty() || !path.peekFirst().equals(source)) {
            return unreachable(source, destination);
        }

        double total = dist.get(destination);
        return new RouteResponse(List.copyOf(path), total, "VALID", source, destination,
                "Route found: " + String.join(" -> ", path) + String.format(" (%.1f min)", total));
    }

    private RouteResponse unreachable(String source, String destination) {
        return new RouteResponse(List.copyOf(List.of()), null, "UNREACHABLE", source, destination,
                "No usable path from " + source + " to " + destination
                        + " — the destination is unreachable (missing roads or all connecting roads are closed)");
    }

    private void requireJunction(String id, String role) {
        if (!junctionRepository.existsById(id)) {
            throw new NotFoundException("Unknown junction '" + id + "' as " + role.toLowerCase()
                    + " — create it first via POST /api/junctions");
        }
    }

    /** Directed edge with its current travel time. */
    private record SegmentEdge(String to, Double travelTime) {
    }

    /** Immutable min-heap record: the key is fixed at insertion time (as in the original). */
    private record HeapEntry(String node, double distance) {
    }
}
