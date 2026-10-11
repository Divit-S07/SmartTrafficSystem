package com.vegam.traffic.model;

/**
 * Kind of traffic event. Values match the TypeScript {@code EventType} union
 * (CLOSE in the Java console app maps to CLOSURE here).
 */
public enum EventType {
    CLOSURE,
    REOPEN,
    CONGESTION
}
