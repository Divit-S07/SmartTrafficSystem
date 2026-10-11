package com.vegam.traffic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vegam.traffic.model.Junction;
import com.vegam.traffic.repository.JunctionRepository;
import com.vegam.traffic.repository.RoadSegmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for every road endpoint, run against the real local
 * MongoDB. Junction fixtures (J1, J2, J3) are seeded per test and everything
 * is deleted afterwards, so no sample data remains.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoadApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JunctionRepository junctionRepository;

    @Autowired
    private RoadSegmentRepository roadRepository;

    @BeforeEach
    void seedJunctions() {
        roadRepository.deleteAll();
        junctionRepository.deleteAll();
        junctionRepository.save(new Junction("J1", 0.0, 0.0));
        junctionRepository.save(new Junction("J2", 1.0, 1.0));
        junctionRepository.save(new Junction("J3", 2.0, 2.0));
    }

    @AfterEach
    void cleanDatabase() {
        roadRepository.deleteAll();
        junctionRepository.deleteAll();
    }

    // ---- helpers -------------------------------------------------------------

    private void createRoad(String json) throws Exception {
        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    private String roadId(String from, String to) {
        return roadRepository.findByFromAndTo(from, to).orElseThrow().getId();
    }

    // ---- GET /api/roads ------------------------------------------------------

    @Test
    void listRoadsEmptyReturns200() throws Exception {
        mockMvc.perform(get("/api/roads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listRoadsReturnsAllSegmentsSorted() throws Exception {
        createRoad("{\"from\":\"J2\",\"to\":\"J3\",\"travelTime\":4,\"oneWay\":true}");
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}");

        mockMvc.perform(get("/api/roads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].from").value("J1"))
                .andExpect(jsonPath("$[1].from").value("J2"));
    }

    // ---- POST /api/roads -----------------------------------------------------

    @Test
    void createOneWayReturns201WithLocationAndPersistsOneSegment() throws Exception {
        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.from").value("J1"))
                .andExpect(jsonPath("$.to").value("J2"))
                .andExpect(jsonPath("$.baseTravelTime").value(5))
                .andExpect(jsonPath("$.currentTravelTime").value(5))
                .andExpect(jsonPath("$.oneWay").value(true))
                .andExpect(jsonPath("$.closed").value(false));

        assertEquals(1, roadRepository.count(), "one-way road must be exactly one segment");
    }

    @Test
    void createTwoWayReturns201AndCreatesBothDirections() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":false}");

        assertEquals(2, roadRepository.count(), "two-way road must create both directed segments");
        assertTrue(roadRepository.findByFromAndTo("J1", "J2").orElseThrow().getId() != null);
        assertTrue(roadRepository.findByFromAndTo("J2", "J1").orElseThrow().getId() != null);
    }

    @Test
    void createWithUnknownJunctionReturns400() throws Exception {
        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"J1\",\"to\":\"GHOST\",\"travelTime\":5,\"oneWay\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Unknown junction 'GHOST' — create it first via POST /api/junctions"));
        assertEquals(0, roadRepository.count());
    }

    @Test
    void createWithSameFromAndToReturns400() throws Exception {
        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"J1\",\"to\":\"J1\",\"travelTime\":5,\"oneWay\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Source and destination junctions must be different (got 'J1' for both)"));
    }

    @Test
    void createWithNegativeTravelTimeReturns400() throws Exception {
        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":-3,\"oneWay\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "travelTime: Travel time must be a positive number of minutes"));
    }

    @Test
    void createWithMissingDirectionReturns400() throws Exception {
        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "oneWay: Direction type must be given (oneWay=true for one-way, false for two-way)"));
    }

    // ---- POST duplicate rules (edge-overlap, consistent across directions) ---

    @Test
    void createDuplicateOneWayReturns409() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}");

        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":9,\"oneWay\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Road(s) already exist: J1->J2"));
        assertEquals(1, roadRepository.count());
    }

    @Test
    void createOneWayCoveredByExistingTwoWayReturns409() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":false}");

        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Road(s) already exist: J1->J2"));
    }

    @Test
    void createTwoWayWhenOppositeOneWayExistsReturns409() throws Exception {
        createRoad("{\"from\":\"J2\",\"to\":\"J1\",\"travelTime\":3,\"oneWay\":true}");

        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Road(s) already exist: J2->J1 (duplicate detected for the two-way connection J1 <-> J2)"));
    }

    @Test
    void createDuplicateTwoWayReversedOrderReturns409() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":false}");

        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"J2\",\"to\":\"J1\",\"travelTime\":7,\"oneWay\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Road(s) already exist: J2->J1, J1->J2 (duplicate detected for the two-way connection J2 <-> J1)"));
        assertEquals(2, roadRepository.count(), "no partial road may be created");
    }

    // ---- PUT /api/roads/{id} -------------------------------------------------

    @Test
    void updateUnknownRoadReturns404() throws Exception {
        mockMvc.perform(put("/api/roads/NOPE").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"travelTime\":5,\"oneWay\":true,\"closed\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Road 'NOPE' not found"));
    }

    @Test
    void updateMissingFieldReturns400() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}");

        mockMvc.perform(put("/api/roads/" + roadId("J1", "J2")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"travelTime\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "closed: Status must be given (closed=true for closed, false for open); "
                                + "oneWay: Direction type must be given (oneWay=true for one-way, false for two-way)"));
    }

    @Test
    void updateTravelTimeOnTwoWayUpdatesBothSegments() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":false}");

        mockMvc.perform(put("/api/roads/" + roadId("J1", "J2")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"travelTime\":8,\"oneWay\":false,\"closed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseTravelTime").value(8));

        assertEquals(8.0, roadRepository.findByFromAndTo("J2", "J1").orElseThrow().getBaseTravelTime(),
                "reverse segment must follow the new travel time");
    }

    @Test
    void updateMakingOneWayDeletesReverseSegment() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":false}");

        mockMvc.perform(put("/api/roads/" + roadId("J1", "J2")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"travelTime\":5,\"oneWay\":true,\"closed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oneWay").value(true));

        assertEquals(1, roadRepository.count(), "reverse segment must be deleted");
        assertTrue(roadRepository.findByFromAndTo("J2", "J1").isEmpty());
    }

    @Test
    void updateMakingTwoWayCreatesReverseSegment() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}");

        mockMvc.perform(put("/api/roads/" + roadId("J1", "J2")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"travelTime\":6,\"oneWay\":false,\"closed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oneWay").value(false));

        assertEquals(2, roadRepository.count(), "reverse segment must be created");
        assertTrue(roadRepository.findByFromAndTo("J2", "J1").isPresent());
    }

    @Test
    void updateMakingTwoWayWhenReverseOneWayExistsReturns409() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}");
        createRoad("{\"from\":\"J2\",\"to\":\"J1\",\"travelTime\":3,\"oneWay\":true}");

        mockMvc.perform(put("/api/roads/" + roadId("J1", "J2")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"travelTime\":5,\"oneWay\":false,\"closed\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Cannot make road J1->J2 two-way: the reverse road J2->J1 already exists "
                                + "as a separate one-way road. Delete it first."));
        assertEquals(2, roadRepository.count(), "existing roads must be untouched");
    }

    // ---- PATCH /api/roads/{id}/status ---------------------------------------

    @Test
    void patchCloseAndReopenTwoWayAffectsBothSegments() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":false}");
        String id = roadId("J1", "J2");

        mockMvc.perform(patch("/api/roads/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true));
        assertTrue(roadRepository.findByFromAndTo("J2", "J1").orElseThrow().isClosed(),
                "reverse segment must be closed together");

        mockMvc.perform(patch("/api/roads/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$.currentTravelTime").value(5));
        assertFalse(roadRepository.findByFromAndTo("J2", "J1").orElseThrow().isClosed(),
                "reverse segment must be reopened together");
    }

    @Test
    void patchCloseOneWayMarksSegmentClosed() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}");

        mockMvc.perform(patch("/api/roads/" + roadId("J1", "J2") + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true));
    }

    @Test
    void patchUnknownRoadReturns404() throws Exception {
        mockMvc.perform(patch("/api/roads/NOPE/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closed\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Road 'NOPE' not found"));
    }

    @Test
    void patchMissingClosedReturns400() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}");

        mockMvc.perform(patch("/api/roads/" + roadId("J1", "J2") + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "closed: Status must be given (closed=true for closed, false for open)"));
    }

    // ---- DELETE /api/roads/{id} ---------------------------------------------

    @Test
    void deleteOneWayReturns204AndSecondDeleteReturns404() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":true}");
        String id = roadId("J1", "J2");

        mockMvc.perform(delete("/api/roads/" + id)).andExpect(status().isNoContent());
        assertEquals(0, roadRepository.count());

        mockMvc.perform(delete("/api/roads/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Road '" + id + "' not found"));
    }

    @Test
    void deleteTwoWayRemovesBothSegments() throws Exception {
        createRoad("{\"from\":\"J1\",\"to\":\"J2\",\"travelTime\":5,\"oneWay\":false}");

        mockMvc.perform(delete("/api/roads/" + roadId("J1", "J2")))
                .andExpect(status().isNoContent());

        assertEquals(0, roadRepository.count(), "both directions must be deleted together");
    }

    @Test
    void deleteUnknownRoadReturns404() throws Exception {
        mockMvc.perform(delete("/api/roads/GHOST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Road 'GHOST' not found"));
    }
}
