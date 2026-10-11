package com.vegam.traffic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vegam.traffic.repository.JunctionRepository;
import com.vegam.traffic.repository.RoadSegmentRepository;
import com.vegam.traffic.repository.TrafficEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Traffic event endpoint tests. Junctions and roads are created through the
 * public API; everything is removed afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrafficEventRepository eventRepository;

    @Autowired
    private JunctionRepository junctionRepository;

    @Autowired
    private RoadSegmentRepository roadRepository;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        eventRepository.deleteAll();
        roadRepository.deleteAll();
        junctionRepository.deleteAll();
        junction("J1");
        junction("J2");
        junction("J3");
    }

    private void junction(String id) {
        junctionRepository.save(new com.vegam.traffic.model.Junction(id, 0.0, 0.0));
    }

    private void road(String from, String to, boolean oneWay) throws Exception {
        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + from + "\",\"to\":\"" + to
                                + "\",\"travelTime\":4,\"oneWay\":" + oneWay + "}"))
                .andExpect(status().isCreated());
    }

    private void createEvent(String json) throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    private void patchStatus(String id, String status) throws Exception {
        mockMvc.perform(patch("/api/events/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    // ---- creation ------------------------------------------------------------

    @Test
    void createClosureReturns201WithFifoSequence() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CLOSURE\",\"from\":\"J1\",\"to\":\"J2\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.sequence").value(1))
                .andExpect(jsonPath("$.type").value("CLOSURE"))
                .andExpect(jsonPath("$.reopening").value(false))
                .andExpect(jsonPath("$.description").value("CLOSE J1->J2"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReopenMarksReopeningTrue() throws Exception {
        createEvent("{\"type\":\"REOPEN\",\"from\":\"J1\",\"to\":\"J2\"}");
        mockMvc.perform(get("/api/events"))
                .andExpect(jsonPath("$[0].reopening").value(true))
                .andExpect(jsonPath("$[0].description").value("REOPEN J1->J2"));
    }

    @Test
    void createCongestionRequiresAndStoresFactor() throws Exception {
        createEvent("{\"type\":\"CONGESTION\",\"from\":\"J1\",\"to\":\"J2\",\"factor\":1.5}");
        mockMvc.perform(get("/api/events"))
                .andExpect(jsonPath("$[0].factor").value(1.5))
                .andExpect(jsonPath("$[0].description").value("CONGESTION J1->J2 x1.5"));
    }

    @Test
    void sequencesIncreaseStrictlyInArrivalOrder() throws Exception {
        createEvent("{\"type\":\"CLOSURE\",\"from\":\"J1\",\"to\":\"J2\"}");
        createEvent("{\"type\":\"CONGESTION\",\"from\":\"J2\",\"to\":\"J3\",\"factor\":2.0}");
        createEvent("{\"type\":\"REOPEN\",\"from\":\"J1\",\"to\":\"J2\"}");

        mockMvc.perform(get("/api/events"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].sequence").value(1))
                .andExpect(jsonPath("$[1].sequence").value(2))
                .andExpect(jsonPath("$[2].sequence").value(3));
    }

    // ---- validation ----------------------------------------------------------

    @Test
    void congestionWithoutFactorReturns400() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CONGESTION\",\"from\":\"J1\",\"to\":\"J2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "CONGESTION events require a slowdown factor (e.g. 1.5 = 50% slower)"));
    }

    @Test
    void congestionFactorBelowOneReturns400() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CONGESTION\",\"from\":\"J1\",\"to\":\"J2\",\"factor\":0.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Slowdown factor must be a finite number >= 1.0 (got 0.5)"));
    }

    @Test
    void closureWithFactorReturns400() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CLOSURE\",\"from\":\"J1\",\"to\":\"J2\",\"factor\":1.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Slowdown factor only applies to CONGESTION events"));
    }

    @Test
    void sameFromAndToReturns400() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CLOSURE\",\"from\":\"J1\",\"to\":\"J1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Source and destination junctions must be different (got 'J1' for both)"));
    }

    @Test
    void unknownJunctionReturns404() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CLOSURE\",\"from\":\"J1\",\"to\":\"GHOST\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- retrieval -----------------------------------------------------------

    @Test
    void getEventReturns200AndUnknownReturns404() throws Exception {
        createEvent("{\"type\":\"CLOSURE\",\"from\":\"J1\",\"to\":\"J2\"}");
        String id = eventRepository.findTopByOrderBySequenceDesc().orElseThrow().getId();

        mockMvc.perform(get("/api/events/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/api/events/GHOST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Event 'GHOST' not found"));
    }

    // ---- processing (apply-on-process, Java semantics) -----------------------

    @Test
    void processingClosureClosesTheRoad() throws Exception {
        road("J1", "J2", false);
        createEvent("{\"type\":\"CLOSURE\",\"from\":\"J1\",\"to\":\"J2\"}");
        String id = eventRepository.findTopByOrderBySequenceDesc().orElseThrow().getId();

        patchStatus(id, "PROCESSED");

        assertTrue(roadRepository.findByFromAndTo("J1", "J2").orElseThrow().isClosed(),
                "processing a CLOSURE must close the directed segment");
        assertEquals("PROCESSED", eventRepository.findById(id).orElseThrow().getStatus().name());
    }

    @Test
    void processingReopenReopensRoadAndRestoresBaseTravelTime() throws Exception {
        road("J1", "J2", false);
        String roadId = roadRepository.findByFromAndTo("J1", "J2").orElseThrow().getId();
        mockMvc.perform(patch("/api/roads/" + roadId + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closed\":true}")).andExpect(status().isOk());
        createEvent("{\"type\":\"REOPEN\",\"from\":\"J1\",\"to\":\"J2\"}");
        String id = eventRepository.findTopByOrderBySequenceDesc().orElseThrow().getId();

        patchStatus(id, "PROCESSED");

        var segment = roadRepository.findByFromAndTo("J1", "J2").orElseThrow();
        assertFalse(segment.isClosed(), "processing a REOPEN must reopen the segment");
        assertEquals(4.0, segment.getCurrentTravelTime(), "reopen must restore the base travel time");
    }

    @Test
    void processingCongestionMultipliesBaseTravelTime() throws Exception {
        road("J1", "J2", false);
        createEvent("{\"type\":\"CONGESTION\",\"from\":\"J1\",\"to\":\"J2\",\"factor\":1.5}");
        String id = eventRepository.findTopByOrderBySequenceDesc().orElseThrow().getId();

        patchStatus(id, "PROCESSED");

        assertEquals(6.0, roadRepository.findByFromAndTo("J1", "J2").orElseThrow().getCurrentTravelTime(),
                "1.5x congestion on a 4 min road must yield 6 min");
    }

    @Test
    void processingCongestionIsIdempotentNotCompounded() throws Exception {
        road("J1", "J2", false);
        createEvent("{\"type\":\"CONGESTION\",\"from\":\"J1\",\"to\":\"J2\",\"factor\":1.5}");
        String id = eventRepository.findTopByOrderBySequenceDesc().orElseThrow().getId();

        patchStatus(id, "PROCESSED");
        patchStatus(id, "PROCESSED"); // re-processing must not compound (Java: current = base * factor)

        assertEquals(6.0, roadRepository.findByFromAndTo("J1", "J2").orElseThrow().getCurrentTravelTime());
    }

    @Test
    void processingEventWithoutRoadReturns409() throws Exception {
        createEvent("{\"type\":\"CLOSURE\",\"from\":\"J2\",\"to\":\"J3\"}"); // no road J2->J3 exists
        String id = eventRepository.findTopByOrderBySequenceDesc().orElseThrow().getId();

        mockMvc.perform(patch("/api/events/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "No road J2->J3 to apply CLOSURE to (road may have been deleted)"));
    }

    @Test
    void revertingToPendingKeepsAppliedRoadChanges() throws Exception {
        road("J1", "J2", false);
        createEvent("{\"type\":\"CLOSURE\",\"from\":\"J1\",\"to\":\"J2\"}");
        String id = eventRepository.findTopByOrderBySequenceDesc().orElseThrow().getId();

        patchStatus(id, "PROCESSED");
        patchStatus(id, "PENDING");

        assertTrue(roadRepository.findByFromAndTo("J1", "J2").orElseThrow().isClosed(),
                "re-queueing must not revert road changes (Java never reverts either)");
    }

    @Test
    void invalidStatusValueReturns400() throws Exception {
        createEvent("{\"type\":\"CLOSURE\",\"from\":\"J1\",\"to\":\"J2\"}");
        String id = eventRepository.findTopByOrderBySequenceDesc().orElseThrow().getId();

        mockMvc.perform(patch("/api/events/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
    }
}
