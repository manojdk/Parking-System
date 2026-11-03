package com.parkingSystem.service;

import com.parkingSystem.dto.AvailabilitySummaryDTO;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.VehicleType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Read-only slot lookups for drivers. Nothing here changes a space; entry and reservations do that.
 */
public interface ISlotAllocationService {

    /**
     * Find nearest available slot by location and type
     */
    Optional<ParkingSpace> findNearestAvailableSlot(String location, SlotType slotType);

    Page<ParkingSpace> findAvailableSlotsByType(SlotType slotType, Pageable pageable);

    Page<ParkingSpace> findAvailableSlotsByLocation(String location, Pageable pageable);

    Page<ParkingSpace> getAllAvailableSlots(Pageable pageable);

    /**
     * Free spaces per slot type
     */
    AvailabilitySummaryDTO getAvailabilitySummary();

    /**
     * Nearest free slot at the location that suits the vehicle, trying its recommended slot types in order
     * (for an EV: a charging bay first, then a normal one).
     */
    Optional<ParkingSpace> recommendSlot(String location, VehicleType vehicleType);

    /**
     * Check if slot is available (in service and AVAILABLE)
     */
    boolean isSlotAvailable(Long parkingSpaceId);

    /**
     * Get slots by priority
     */
    List<ParkingSpace> getSlotsByPriority(SlotType slotType, String location);
}
