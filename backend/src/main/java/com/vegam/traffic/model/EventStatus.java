package com.vegam.traffic.model;

/**
 * Queue state of a traffic event: events arrive as PENDING and are processed
 * strictly FIFO, after which they remain stored as PROCESSED.
 * Matches the TypeScript {@code EventStatus} union.
 */
public enum EventStatus {
    PENDING,
    PROCESSED
}
