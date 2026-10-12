package com.parkingSystem.enums;

/**
 * Reservation life cycle. ACTIVE is the only open state; the other three are final.
 */
public enum ReservationStatus {
    /** Holding the space until the owner arrives or the hold runs out. */
    ACTIVE("Active"),
    /** The owner drove in; the reservation is linked to the parking ticket. */
    FULFILLED("Fulfilled"),
    /** Cancelled by the owner or an admin before arrival. */
    CANCELLED("Cancelled"),
    /** The hold ran out before the owner arrived (no-show). */
    EXPIRED("Expired");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
