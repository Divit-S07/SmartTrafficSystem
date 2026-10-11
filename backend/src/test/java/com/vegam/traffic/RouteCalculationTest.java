package com.vegam.traffic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultActions;

/**
 * Route calculation tests. Every junction and road is created through the
 * public API (POST /api/junctions, POST /api/roads, PATCH .../status) — no
 * fixtures are written straight to the database, and everything is removed
 * afterwards so no sample data remains.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RouteCalculationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JunctionRepository junctionRepository;

    @Autowired
    private RoadSegmentRepository roadRepository;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        roadRepository.deleteAll();
        junctionRepository.deleteAll();
    }

    // ---- helpers: records created through the API ---------------------------

    private void junction(String id) throws Exception {
        mockMvc.perform(post("/api/junctions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\",\"x\":0,\"y\":0}"))
                .andExpect(status().isCreated());
    }

    private void road(String from, String to, double travelTime, boolean oneWay) throws Exception {
        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + from + "\",\"to\":\"" + to + "\",\"travelTime\":"
                                + travelTime + ",\"oneWay\":" + oneWay + "}"))
                .andExpect(status().isCreated());
    }

    private void closeRoad(String from, String to) throws Exception {
        String id = roadRepository.findByFromAndTo(from, to).orElseThrow().getId();
        mockMvc.perform(patch("/api/roads/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closed\":true}"))
                .andExpect(status().isOk());
    }

    private ResultActions calculate(String source, String destination) throws Exception {
        return mockMvc.perform(post("/api/routes/calculate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"" + source + "\",\"destination\":\"" + destination + "\"}"))
                .andExpect(status().isOk());
    }

    // ---- happy path ----------------------------------------------------------

    @Test
    void findsShortestPathByTravelTimeNotByHopCount() throws Exception {
        junction("J1");
        junction("J2");
        junction("J3");
        road("J1", "J2", 2, false);   // 2 + 2 = 4 min via J2
        road("J2", "J3", 2, false);
        road("J1", "J3", 10, false);  // 10 min direct

        calculate("J1", "J3")
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.path[0]").value("J1"))
                .andExpect(jsonPath("$.path[1]").value("J2"))
                .andExpect(jsonPath("$.path[2]").value("J3"))
                .andExpect(jsonPath("$.totalTravelTime").value(4.0))
                .andExpect(jsonPath("$.requestedFrom").value("J1"))
                .andExpect(jsonPath("$.requestedTo").value("J3"))
                .andExpect(jsonPath("$.message").value("Route found: J1 -> J2 -> J3 (4.0 min)"));
    }

    @Test
    void sameSourceAndDestinationReturnsZeroLengthRoute() throws Exception {
        junction("J1");

        calculate("J1", "J1")
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.path.length()").value(1))
                .andExpect(jsonPath("$.path[0]").value("J1"))
                .andExpect(jsonPath("$.totalTravelTime").value(0.0));
    }

    // ---- directionality ------------------------------------------------------

    @Test
    void oneWayRoadPermitsTravelOnlyInConfiguredDirection() throws Exception {
        junction("J1");
        junction("J2");
        road("J1", "J2", 5, true); // one-way J1 -> J2, no reverse edge

        calculate("J1", "J2")
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.path[0]").value("J1"))
                .andExpect(jsonPath("$.path[1]").value("J2"))
                .andExpect(jsonPath("$.totalTravelTime").value(5.0));

        // The reversed request must not use the one-way road backwards.
        calculate("J2", "J1")
                .andExpect(jsonPath("$.status").value("UNREACHABLE"))
                .andExpect(jsonPath("$.path.length()").value(0))
                .andExpect(jsonPath("$.totalTravelTime").doesNotExist())
                .andExpect(jsonPath("$.message").value(
                        "No usable path from J2 to J1 — the destination is unreachable "
                                + "(missing roads or all connecting roads are closed)"));
    }

    @Test
    void twoWayRoadPermitsBothDirections() throws Exception {
        junction("J1");
        junction("J2");
        road("J1", "J2", 5, false);

        calculate("J1", "J2").andExpect(jsonPath("$.status").value("VALID"));
        calculate("J2", "J1")
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.totalTravelTime").value(5.0));
    }

    // ---- closed roads excluded ----------------------------------------------

    @Test
    void closedRoadIsExcludedAndDetourIsChosen() throws Exception {
        junction("J1");
        junction("J2");
        junction("J3");
        road("J1", "J2", 2, false);
        road("J2", "J3", 2, false);
        road("J1", "J3", 10, false);
        closeRoad("J1", "J2"); // closes both directions of the two-way road

        calculate("J1", "J3")
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.path.length()").value(2))
                .andExpect(jsonPath("$.path[1]").value("J3"))
                .andExpect(jsonPath("$.totalTravelTime").value(10.0));
    }

    @Test
    void closureIsolatingDestinationReportsUnreachableWithUsefulMessage() throws Exception {
        // Mirrors scenario 4 of the original Java demonstration: J6 -> J12 is
        // J12's only entrance; closing it isolates J12.
        junction("J6");
        junction("J12");
        road("J6", "J12", 5, true);
        closeRoad("J6", "J12");

        calculate("J6", "J12")
                .andExpect(jsonPath("$.status").value("UNREACHABLE"))
                .andExpect(jsonPath("$.message").value(
                        "No usable path from J6 to J12 — the destination is unreachable "
                                + "(missing roads or all connecting roads are closed)"));
    }

    @Test
    void reopenedRoadIsUsableAgain() throws Exception {
        junction("J1");
        junction("J2");
        road("J1", "J2", 5, true);
        closeRoad("J1", "J2");
        calculate("J1", "J2").andExpect(jsonPath("$.status").value("UNREACHABLE"));

        String id = roadRepository.findByFromAndTo("J1", "J2").orElseThrow().getId();
        mockMvc.perform(patch("/api/roads/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closed\":false}"))
                .andExpect(status().isOk());

        calculate("J1", "J2").andExpect(jsonPath("$.status").value("VALID"));
    }

    // ---- validation ----------------------------------------------------------

    @Test
    void unknownSourceReturns404() throws Exception {
        junction("J1");

        mockMvc.perform(post("/api/routes/calculate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"GHOST\",\"destination\":\"J1\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(
                        "Unknown junction 'GHOST' as source — create it first via POST /api/junctions"));
    }

    @Test
    void unknownDestinationReturns404() throws Exception {
        junction("J1");

        mockMvc.perform(post("/api/routes/calculate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"J1\",\"destination\":\"NOWHERE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(
                        "Unknown junction 'NOWHERE' as destination — create it first via POST /api/junctions"));
    }

    @Test
    void blankFieldsReturn400() throws Exception {
        mockMvc.perform(post("/api/routes/calculate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"\",\"destination\":\"J1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("source: Source junction must not be blank"));
    }

    @Test
    void twoJunctionsWithoutRoadsReportUnreachable() throws Exception {
        junction("J1");
        junction("J2");

        calculate("J1", "J2")
                .andExpect(jsonPath("$.status").value("UNREACHABLE"))
                .andExpect(jsonPath("$.path.length()").value(0));
    }

    @Test
    void junctionsSurviveCalculationsUntouched() throws Exception {
        junction("J1");
        junction("J2");
        road("J1", "J2", 5, true);

        calculate("J1", "J2");

        assertEquals(2, junctionRepository.count(), "calculation must not modify junctions");
        assertEquals(1, roadRepository.count(), "calculation must not modify roads");
        assertEquals(false, roadRepository.findByFromAndTo("J1", "J2").orElseThrow().isClosed(),
                "calculation must not modify road status");
    }
}
