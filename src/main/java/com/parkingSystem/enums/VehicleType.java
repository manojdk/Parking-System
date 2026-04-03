package com.parkingSystem.enums;

/**
 * Vehicle types supported in the parking system
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
}

