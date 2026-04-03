package com.parkingSystem.service;

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.enums.SlotType;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for intelligent slot allocation
 */
public interface ISlotAllocationService {

    /**
     * Find nearest available slot by location and type
     */
    Optional<ParkingSpace> findNearestAvailableSlot(String location, SlotType slotType);

    /**
     * Find available slot by type only
     */
    List<ParkingSpace> findAvailableSlotsByType(SlotType slotType);

    /**
     * Find available slots by location
     */
    List<ParkingSpace> findAvailableSlotsByLocation(String location);

    /**
     * Find all available slots
     */
    List<ParkingSpace> getAllAvailableSlots();

    /**
     * Get availability summary
     */
    AvailabilitySummary getAvailabilitySummary();

    /**
     * Recommend slot based on vehicle type
     */
    Optional<ParkingSpace> recommendSlot(String location, String vehicleType);

    /**
     * Check if slot is available
     */
    boolean isSlotAvailable(Long parkingSpaceId);

    /**
     * Allocate slot
     */
    ParkingSpace allocateSlot(Long parkingSpaceId);

    /**
     * Release slot
     */
    void releaseSlot(Long parkingSpaceId);

    /**
     * Get slots by priority
     */
    List<ParkingSpace> getSlotsByPriority(SlotType slotType, String location);
}
