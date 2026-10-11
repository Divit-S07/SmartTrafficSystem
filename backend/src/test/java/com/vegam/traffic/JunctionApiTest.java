package com.vegam.traffic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vegam.traffic.model.RoadSegment;
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
 * Integration tests for every junction endpoint, run against the real local
 * MongoDB. All fixtures are deleted after each test, so no sample data
 * remains in the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JunctionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JunctionRepository junctionRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        roadSegmentRepository.deleteAll();
        junctionRepository.deleteAll();
    }

    // ---- POST /api/junctions -------------------------------------------------

    @Test
    void createJunctionReturns201WithLocationAndBody() throws Exception {
        mockMvc.perform(post("/api/junctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"J1\",\"x\":10.5,\"y\":20.0}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/junctions/J1"))
                .andExpect(jsonPath("$.id").value("J1"))
                .andExpect(jsonPath("$.x").value(10.5))
                .andExpect(jsonPath("$.y").value(20.0));

        assertTrue(junctionRepository.existsById("J1"), "junction must be persisted");
    }

    @Test
    void createJunctionWithDuplicateIdReturns409() throws Exception {
        junctionRepository.save(new com.vegam.traffic.model.Junction("J1", 1.0, 2.0));

        mockMvc.perform(post("/api/junctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"J1\",\"x\":3,\"y\":4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Junction 'J1' already exists"));
    }

    @Test
    void createJunctionWithBlankIdReturns400() throws Exception {
        mockMvc.perform(post("/api/junctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"  \",\"x\":1,\"y\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "id: Junction id must not be blank"));

        assertEquals(0, junctionRepository.count(), "nothing must be persisted");
    }

    @Test
    void createJunctionWithMissingCoordinateReturns400() throws Exception {
        mockMvc.perform(post("/api/junctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"J2\",\"x\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "y: Junction y coordinate must not be null"));
    }

    @Test
    void createJunctionWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/junctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/junctions --------------------------------------------------

    @Test
    void listJunctionsReturns200AndAllJunctionsSortedById() throws Exception {
        junctionRepository.save(new com.vegam.traffic.model.Junction("J2", 2.0, 2.0));
        junctionRepository.save(new com.vegam.traffic.model.Junction("J1", 1.0, 1.0));

        mockMvc.perform(get("/api/junctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("J1"))
                .andExpect(jsonPath("$[1].id").value("J2"));
    }

    @Test
    void listJunctionsReturnsEmptyArrayWhenNoneExist() throws Exception {
        mockMvc.perform(get("/api/junctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- GET /api/junctions/{id} --------------------------------------------

    @Test
    void getJunctionReturns200() throws Exception {
        junctionRepository.save(new com.vegam.traffic.model.Junction("J1", 7.0, 8.0));

        mockMvc.perform(get("/api/junctions/J1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("J1"))
                .andExpect(jsonPath("$.x").value(7.0))
                .andExpect(jsonPath("$.y").value(8.0));
    }

    @Test
    void getUnknownJunctionReturns404() throws Exception {
        mockMvc.perform(get("/api/junctions/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Junction 'NOPE' not found"));
    }

    // ---- PUT /api/junctions/{id} --------------------------------------------

    @Test
    void updateJunctionPositionReturns200AndPersistsNewCoordinates() throws Exception {
        junctionRepository.save(new com.vegam.traffic.model.Junction("J1", 1.0, 1.0));

        // This is what the React Flow map sends after a drag ends.
        mockMvc.perform(put("/api/junctions/J1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":700.0,\"y\":90.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("J1"))
                .andExpect(jsonPath("$.x").value(700.0))
                .andExpect(jsonPath("$.y").value(90.0));

        com.vegam.traffic.model.Junction saved = junctionRepository.findById("J1").orElseThrow();
        assertEquals(700.0, saved.getX(), "position must be persisted");
        assertEquals(90.0, saved.getY(), "position must be persisted");
    }

    @Test
    void updateJunctionPositionLeavesReferencingRoadsUntouched() throws Exception {
        junctionRepository.save(new com.vegam.traffic.model.Junction("J1", 1.0, 1.0));
        junctionRepository.save(new com.vegam.traffic.model.Junction("J2", 2.0, 2.0));
        roadSegmentRepository.save(new RoadSegment("J1", "J2", 5.0, 5.0, false, false));

        mockMvc.perform(put("/api/junctions/J1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":50.0,\"y\":60.0}"))
                .andExpect(status().isOk());

        // Routing topology must be unchanged by a layout-only edit.
        assertEquals(1, roadSegmentRepository.count(), "roads must not be touched");
        assertEquals(5.0, roadSegmentRepository.findAll().get(0).getBaseTravelTime(),
                "travel time must not be touched");
    }

    @Test
    void updateUnknownJunctionPositionReturns404() throws Exception {
        mockMvc.perform(put("/api/junctions/GHOST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":1.0,\"y\":2.0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Junction 'GHOST' not found"));
    }

    @Test
    void updateJunctionPositionWithMissingCoordinateReturns400() throws Exception {
        junctionRepository.save(new com.vegam.traffic.model.Junction("J1", 1.0, 1.0));

        mockMvc.perform(put("/api/junctions/J1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":5.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "y: Junction y coordinate must not be null"));
    }

    // ---- DELETE /api/junctions/{id} -----------------------------------------

    @Test
    void deleteJunctionReturns204AndRemovesIt() throws Exception {
        junctionRepository.save(new com.vegam.traffic.model.Junction("J1", 1.0, 1.0));

        mockMvc.perform(delete("/api/junctions/J1"))
                .andExpect(status().isNoContent());

        assertFalse(junctionRepository.existsById("J1"), "junction must be deleted");
    }

    @Test
    void deleteUnknownJunctionReturns404() throws Exception {
        mockMvc.perform(delete("/api/junctions/GHOST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Junction 'GHOST' not found"));
    }

    @Test
    void deleteJunctionReferencedByRoadReturns409AndKeepsEverything() throws Exception {
        junctionRepository.save(new com.vegam.traffic.model.Junction("J1", 1.0, 1.0));
        junctionRepository.save(new com.vegam.traffic.model.Junction("J2", 2.0, 2.0));
        roadSegmentRepository.save(new RoadSegment("J1", "J2", 5.0, 5.0, false, false));

        mockMvc.perform(delete("/api/junctions/J1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Cannot delete junction 'J1': it is referenced by road(s) [J1->J2]. "
                                + "Delete those roads first."));

        assertTrue(junctionRepository.existsById("J1"), "junction must survive the delete");
        assertEquals(1, roadSegmentRepository.count(), "referencing road must survive too");
    }
}
