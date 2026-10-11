package com.vegam.traffic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vegam.traffic.repository.EmergencyVehicleRepository;
import com.vegam.traffic.repository.JunctionRepository;
import com.vegam.traffic.repository.RoadSegmentRepository;
import com.vegam.traffic.repository.TrafficEventRepository;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.MongoDatabaseFactory;


/**
 * Verifies the MongoDB connection configured in application.yml without
 * inserting any sample data:
 * <ol>
 *   <li>the driver answers a ping on the configured database,</li>
 *   <li>the resolved database name is exactly {@code smart_traffic_db},</li>
 *   <li>all four repositories are wired and all collections are empty,</li>
 *   <li>the unique constraints (compound index on road endpoints, unique
 *       event sequence) were actually created by auto-index-creation.</li>
 * </ol>
 */
@SpringBootTest
class MongoConnectionTest {

    @Autowired
    private MongoDatabaseFactory mongoDatabaseFactory;

    @Autowired
    private JunctionRepository junctionRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private EmergencyVehicleRepository emergencyVehicleRepository;

    @Autowired
    private TrafficEventRepository trafficEventRepository;

    @Test
    void connectsToSmartTrafficDb() {
        MongoDatabase db = mongoDatabaseFactory.getMongoDatabase();
        assertEquals("smart_traffic_db", db.getName(),
                "Configured database must be smart_traffic_db");
        // MongoDB 8.0+ no longer echoes the "ping" field back; the command
        // response only contains ok:1, so assert on that (present in all versions).
        Document pong = db.runCommand(new Document("ping", 1));
        assertEquals(1.0, ((Number) pong.get("ok")).doubleValue(),
                "MongoDB ping must return ok=1");
    }

    @Test
    void repositoriesAreWiredAndNoDataWasInserted() {
        assertEquals(0, junctionRepository.count(), "junctions must be empty (no sample data)");
        assertEquals(0, roadSegmentRepository.count(), "road_segments must be empty (no sample data)");
        assertEquals(0, emergencyVehicleRepository.count(), "emergency_vehicles must be empty (no sample data)");
        assertEquals(0, trafficEventRepository.count(), "traffic_events must be empty (no sample data)");
    }

    @Test
    void uniqueConstraintsWereCreated() {
        assertUniqueIndexExists("road_segments", new Document("from", 1).append("to", 1));
        assertUniqueIndexExists("traffic_events", new Document("sequence", 1));
    }

    /** Asserts, via the driver's listIndexes, that an index with the expected keys exists and is unique. */
    private void assertUniqueIndexExists(String collection, Document expectedKeys) {
        MongoDatabase db = mongoDatabaseFactory.getMongoDatabase();
        boolean found = false;
        for (Document index : db.getCollection(collection).listIndexes()) {
            if (expectedKeys.equals(index.get("key"))) {
                found = true;
                assertEquals(Boolean.TRUE, index.get("unique"),
                        collection + " index '" + index.get("name") + "' must be unique");
            }
        }
        assertTrue(found, collection + " must have an index on " + expectedKeys
                + " (auto-index-creation may be disabled)");
    }
}
