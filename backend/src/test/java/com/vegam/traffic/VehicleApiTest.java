package com.vegam.traffic;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vegam.traffic.repository.EmergencyVehicleRepository;
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
 * Emergency vehicle endpoint tests. Junctions and roads are created through
 * the public API; everything is removed afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VehicleApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmergencyVehicleRepository vehicleRepository;

    @Autowired
    private JunctionRepository junctionRepository;

    @Autowired
    private RoadSegmentRepository roadRepository;

    @Autowired
    private TrafficEventRepository eventRepository;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        eventRepository.deleteAll();
        vehicleRepository.deleteAll();
        roadRepository.deleteAll();
        junctionRepository.deleteAll();
    }

    private void junction(String id) throws Exception {
        mockMvc.perform(post("/api/junctions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\",\"x\":0,\"y\":0}"))
                .andExpect(status().isCreated());
    }

    private void road(String from, String to, boolean oneWay) throws Exception {
        mockMvc.perform(post("/api/roads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + from + "\",\"to\":\"" + to + "\",\"travelTime\":5,\"oneWay\":" + oneWay + "}"))
                .andExpect(status().isCreated());
    }

    private void register(String json) throws Exception {
        mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    private void closeRoad(String from, String to) throws Exception {
        String id = roadRepository.findByFromAndTo(from, to).orElseThrow().getId();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/roads/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"closed\":true}"))
                .andExpect(status().isOk());
    }

    // ---- registration --------------------------------------------------------

    @Test
    void registerReturns201WithLocationAndDefaultsToNormalPriority() throws Exception {
        junction("J1");
        mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/vehicles/AMB1"))
                .andExpect(jsonPath("$.id").value("AMB1"))
                .andExpect(jsonPath("$.type").value("AMBULANCE"))
                .andExpect(jsonPath("$.lastConfirmedJunction").value("J1"))
                .andExpect(jsonPath("$.status").value("IDLE"))
                .andExpect(jsonPath("$.priority").value("NORMAL"));
    }

    @Test
    void registerWithExplicitPriorityReturns201() throws Exception {
        junction("J1");
        register("{\"id\":\"FIRE1\",\"type\":\"FIRE_ENGINE\",\"junction\":\"J1\",\"priority\":\"CRITICAL\"}");
        mockMvc.perform(get("/api/vehicles/FIRE1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
    }

    @Test
    void registerDuplicateIdReturns409() throws Exception {
        junction("J1");
        register("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}");

        mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"AMB1\",\"type\":\"RESCUE\",\"junction\":\"J1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Vehicle 'AMB1' is already registered"));
    }

    @Test
    void registerMissingTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"AMB1\",\"junction\":\"J1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("type: Vehicle type must not be null"));
    }

    @Test
    void registerBlankIdReturns400() throws Exception {
        mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\" \",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("id: Vehicle id must not be blank"));
    }

    @Test
    void registerUnknownJunctionReturns404() throws Exception {
        mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"GHOST\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(
                        "Unknown junction 'GHOST' as starting junction — create it first via POST /api/junctions"));
    }

    // ---- listing in emergency-priority order ---------------------------------

    @Test
    void listReturnsPriorityOrderWithIdTieBreak() throws Exception {
        junction("J1");
        register("{\"id\":\"Z1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\",\"priority\":\"NORMAL\"}");
        register("{\"id\":\"M1\",\"type\":\"FIRE_ENGINE\",\"junction\":\"J1\",\"priority\":\"CRITICAL\"}");
        register("{\"id\":\"A1\",\"type\":\"RESCUE\",\"junction\":\"J1\",\"priority\":\"HIGH\"}");
        register("{\"id\":\"Z2\",\"type\":\"AMBULANCE\",\"junction\":\"J1\",\"priority\":\"CRITICAL\"}");

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value("M1"))  // CRITICAL first, id tie-break
                .andExpect(jsonPath("$[1].id").value("Z2"))
                .andExpect(jsonPath("$[2].id").value("A1"))  // HIGH second
                .andExpect(jsonPath("$[3].id").value("Z1")); // NORMAL last
    }

    @Test
    void getUnknownVehicleReturns404() throws Exception {
        mockMvc.perform(get("/api/vehicles/GHOST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Vehicle 'GHOST' not found"));
    }

    // ---- junction updates (Java updatePosition rules) -------------------------

    @Test
    void updateJunctionThroughOpenTwoWayRoadReturns200() throws Exception {
        junction("J1");
        junction("J2");
        road("J1", "J2", false);
        register("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}");

        mockMvc.perform(put("/api/vehicles/AMB1/junction").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"junction\":\"J2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastConfirmedJunction").value("J2"));
    }

    @Test
    void updateJunctionToSameJunctionIsIdempotent() throws Exception {
        junction("J1");
        register("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}");

        mockMvc.perform(put("/api/vehicles/AMB1/junction").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"junction\":\"J1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastConfirmedJunction").value("J1"));
    }

    @Test
    void updateJunctionToNonAdjacentJunctionReturns409() throws Exception {
        junction("J1");
        junction("J2");
        junction("J3");
        register("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}");

        mockMvc.perform(put("/api/vehicles/AMB1/junction").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"junction\":\"J3\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "J3 is not directly reachable from J1 (no open road in that direction)"));
    }

    @Test
    void updateJunctionAgainstOneWayDirectionReturns409() throws Exception {
        // Java demonstration scenario 5: J5->J11 exists, but J11->J5 does not.
        junction("J5");
        junction("J11");
        road("J5", "J11", true);
        register("{\"id\":\"AMB2\",\"type\":\"AMBULANCE\",\"junction\":\"J11\"}");

        mockMvc.perform(put("/api/vehicles/AMB2/junction").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"junction\":\"J5\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateJunctionThroughClosedRoadReturns409() throws Exception {
        junction("J1");
        junction("J2");
        road("J1", "J2", false);
        closeRoad("J1", "J2");
        register("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}");

        mockMvc.perform(put("/api/vehicles/AMB1/junction").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"junction\":\"J2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Road J1 -> J2 is closed; vehicle cannot use it"));
    }

    @Test
    void updateJunctionToUnknownJunctionReturns404() throws Exception {
        junction("J1");
        register("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}");

        mockMvc.perform(put("/api/vehicles/AMB1/junction").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"junction\":\"NOWHERE\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- priority updates ----------------------------------------------------

    @Test
    void updatePriorityReturns200AndChangesListOrder() throws Exception {
        junction("J1");
        register("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}");
        register("{\"id\":\"FIRE1\",\"type\":\"FIRE_ENGINE\",\"junction\":\"J1\",\"priority\":\"CRITICAL\"}");

        mockMvc.perform(put("/api/vehicles/AMB1/priority").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"CRITICAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("CRITICAL"));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(jsonPath("$[0].id").value("AMB1"))
                .andExpect(jsonPath("$[1].id").value("FIRE1"));
    }

    @Test
    void updatePriorityInvalidValueReturns400() throws Exception {
        junction("J1");
        register("{\"id\":\"AMB1\",\"type\":\"AMBULANCE\",\"junction\":\"J1\"}");

        mockMvc.perform(put("/api/vehicles/AMB1/priority").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"LOW\"}"))
                .andExpect(status().isBadRequest());
    }
}
