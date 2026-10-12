package com.parkingSystem.enums;

import java.util.List;

/**
 * Vehicle types supported in the parking system.
 */
public enum VehicleType {
    CAR("Car"),
    BIKE("Bike"),
    EV("Electric Vehicle");

    private final String displayName;

    VehicleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Slot types the recommender offers, best first. VIP and HANDICAPPED bays accept cars too, but are never
     * suggested automatically: they are for drivers who choose them.
     */
    public List<SlotType> recommendedSlotTypes() {
        return switch (this) {
            case CAR -> List.of(SlotType.NORMAL);
            case EV -> List.of(SlotType.EV_CHARGING, SlotType.NORMAL);
            case BIKE -> List.of(SlotType.BIKE);
        };
    }

    /** Case-insensitive parse; unknown values raise IllegalArgumentException (returned as 400). */
    public static VehicleType fromValue(String value) {
        if (value != null) {
            for (VehicleType type : values()) {
                if (type.name().equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown vehicle type: " + value + ". Allowed: CAR, BIKE, EV");
    }
}
