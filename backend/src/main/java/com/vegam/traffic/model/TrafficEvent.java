package com.vegam.traffic.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A queued traffic event (road closure, reopening or congestion), matching the
 * TypeScript {@code TrafficEvent} interface. Processing order is strict FIFO
 * by {@code sequence}; the unique index on {@code sequence} guarantees no two
 * events can ever claim the same position in the queue.
 */
@Document(collection = "traffic_events")
public class TrafficEvent {

    @Id
    private String id;

    @NotNull(message = "Event sequence must not be null")
    @Min(value = 1, message = "Event sequence must be at least 1")
    @Indexed(name = "traffic_events_sequence_unique", unique = true)
    private Integer sequence;

    @NotNull(message = "Event type must not be null")
    private EventType type;

    @NotBlank(message = "Event 'from' junction must not be blank")
    private String from;

    @NotBlank(message = "Event 'to' junction must not be blank")
    private String to;

    /** True for REOPEN events, false for CLOSE/CONGESTION. */
    private boolean reopening;

    /** Congestion slowdown factor (e.g. 1.5 = 50% slower); null unless CONGESTION. */
    @DecimalMin(value = "1.0", message = "Congestion factor must be at least 1.0")
    private Double factor;

    /** Human-readable description, built when the event is queued. */
    private String description;

    @NotNull(message = "Event status must not be null")
    private EventStatus status;

    public TrafficEvent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
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

    public boolean isReopening() {
        return reopening;
    }

    public void setReopening(boolean reopening) {
        this.reopening = reopening;
    }

    public Double getFactor() {
        return factor;
    }

    public void setFactor(Double factor) {
        this.factor = factor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
}
