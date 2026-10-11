package com.vegam.traffic.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A directed, weighted edge of the road network. A two-way road is stored as
 * two documents (one per direction), exactly like the paired directed edges in
 * the original {@code SmartTrafficSystem.java}.
 *
 * <p>No business {@code id} field: the identity of a segment is its
 * {@code (from, to)} pair, enforced by a unique compound index — the database
 * equivalent of the {@code segments} map keyed {@code "from->to"} in the Java
 * original.
 */
@Document(collection = "road_segments")
@CompoundIndexes({
        @CompoundIndex(name = "road_endpoints_unique", def = "{'from': 1, 'to': 1}", unique = true)
})
public class RoadSegment {

    /** MongoDB-generated id; the REST resource address of this directed segment. */
    @Id
    private String id;

    @NotBlank(message = "Road segment 'from' junction must not be blank")
    private String from;

    @NotBlank(message = "Road segment 'to' junction must not be blank")
    private String to;

    @NotNull(message = "Base travel time must not be null")
    @Positive(message = "Base travel time must be a positive number of minutes")
    private Double baseTravelTime;

    @NotNull(message = "Current travel time must not be null")
    @Positive(message = "Current travel time must be a positive number of minutes")
    private Double currentTravelTime;

    private boolean closed;

    private boolean oneWay;

    public RoadSegment() {
    }

    public RoadSegment(String from, String to, Double baseTravelTime, Double currentTravelTime,
                       boolean closed, boolean oneWay) {
        this.from = from;
        this.to = to;
        this.baseTravelTime = baseTravelTime;
        this.currentTravelTime = currentTravelTime;
        this.closed = closed;
        this.oneWay = oneWay;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Double getBaseTravelTime() {
        return baseTravelTime;
    }

    public void setBaseTravelTime(Double baseTravelTime) {
        this.baseTravelTime = baseTravelTime;
    }

    public Double getCurrentTravelTime() {
        return currentTravelTime;
    }

    public void setCurrentTravelTime(Double currentTravelTime) {
        this.currentTravelTime = currentTravelTime;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public void setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
    }
}
