package com.vegam.traffic.repository;

import com.vegam.traffic.model.EventStatus;
import com.vegam.traffic.model.TrafficEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Persistence for {@code traffic_events}. FIFO processing order is guaranteed
 * by the unique {@code sequence} index.
 */
public interface TrafficEventRepository extends MongoRepository<TrafficEvent, String> {

    /** Pending events in strict arrival (FIFO) order, mirroring the Java eventQueue. */
    List<TrafficEvent> findByStatusOrderBySequenceAsc(EventStatus status);

    /** Highest sequence ever assigned — used to continue the FIFO sequence without gaps. */
    Optional<TrafficEvent> findTopByOrderBySequenceDesc();
}
