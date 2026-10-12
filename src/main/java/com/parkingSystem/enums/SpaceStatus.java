package com.parkingSystem.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Where a parking space is in its life cycle.
 * <p>
 * RESERVED and OCCUPIED are owned by the system (reservations and tickets); an admin can only switch a
 * space between AVAILABLE and OUT_OF_SERVICE.
 */
public enum SpaceStatus {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    OCCUPIED("Occupied"),
    OUT_OF_SERVICE("Out of service");

    private final String displayName;

    SpaceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Case-insensitive parse, so the old lower-case values ("available", "occupied") keep working in
     * requests. Unknown values raise IllegalArgumentException, which the API returns as 400.
     */
    @JsonCreator
    public static SpaceStatus fromValue(String value) {
        if (value != null) {
            for (SpaceStatus status : values()) {
                if (status.name().equalsIgnoreCase(value.trim())) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Unknown space status: " + value
                + ". Allowed: AVAILABLE, RESERVED, OCCUPIED, OUT_OF_SERVICE");
    }
}
