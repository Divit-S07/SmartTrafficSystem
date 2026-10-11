package com.vegam.traffic.repository;

import com.vegam.traffic.model.RoadSegment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Persistence for {@code road_segments}. Uniqueness of the directed edge is
 * enforced by the {@code road_endpoints_unique} compound index on (from, to).
 */
public interface RoadSegmentRepository extends MongoRepository<RoadSegment, String> {

    /** Looks up a single directed segment, the DB equivalent of the "from->to" key in the Java original. */
    Optional<RoadSegment> findByFromAndTo(String from, String to);

    /** All segments that start or end at the given junction — used to protect junctions from unsafe deletion. */
    List<RoadSegment> findByFromOrTo(String from, String to);

    /** True when the directed edge already exists (duplicate detection). */
    boolean existsByFromAndTo(String from, String to);
}
