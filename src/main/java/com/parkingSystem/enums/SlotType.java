package com.parkingSystem.enums;

/**
 * Slot types available in the parking system
 */
public enum SlotType {
    NORMAL("Normal Parking"),
    VIP("VIP Parking"),
    HANDICAPPED("Handicapped Parking");

    private final String displayName;

    SlotType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

