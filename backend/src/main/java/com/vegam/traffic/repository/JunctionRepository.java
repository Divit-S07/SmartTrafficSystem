package com.vegam.traffic.repository;

import com.vegam.traffic.model.Junction;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Persistence for {@code junctions}. Junction ids are unique because the
 * business id (e.g. "J1") is the document {@code _id}.
 */
public interface JunctionRepository extends MongoRepository<Junction, String> {
}
