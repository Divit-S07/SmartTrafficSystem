package com.vegam.traffic.model;

/**
 * Emergency priority levels, ported from the original Java {@code Severity}
 * enum with the same rank values (CRITICAL beats HIGH beats NORMAL) — the
 * exact ordering used by the Java priority queue.
 */
public enum Severity {
    CRITICAL(3),
    HIGH(2),
    NORMAL(1);

    private final int rank;

    Severity(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}
