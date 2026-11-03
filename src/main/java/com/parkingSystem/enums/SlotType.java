package com.parkingSystem.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Slot types available in the parking system, and which vehicles each one takes.
 */
public enum SlotType {
    NORMAL("Normal Parking", EnumSet.of(VehicleType.CAR, VehicleType.EV)),
    VIP("VIP Parking", EnumSet.of(VehicleType.CAR, VehicleType.EV)),
    HANDICAPPED("Handicapped Parking", EnumSet.of(VehicleType.CAR, VehicleType.EV)),
    /** Has a charger: reserved for EVs, so a parked petrol car cannot block it. */
    EV_CHARGING("EV Charging", EnumSet.of(VehicleType.EV)),
    /** Two-wheeler bay: too small for cars, and bikes do not take a car bay. */
    BIKE("Two-wheeler Parking", EnumSet.of(VehicleType.BIKE));

    private final String displayName;
    private final Set<VehicleType> accepted;

    SlotType(String displayName, Set<VehicleType> accepted) {
        this.displayName = displayName;
        this.accepted = accepted;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Whether a vehicle of this type may park in a slot of this type. */
    public boolean accepts(VehicleType vehicleType) {
        return vehicleType != null && accepted.contains(vehicleType);
    }
}
