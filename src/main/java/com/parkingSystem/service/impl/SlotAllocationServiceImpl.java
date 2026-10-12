package com.parkingSystem.service.impl;

import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.repository.ParkingSpaceRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for intelligent slot allocation
 */
@Service
@AllArgsConstructor
@Slf4j
public class SlotAllocationServiceImpl implements com.parkingSystem.service.ISlotAllocationService {

    private ParkingSpaceRepository parkingSpaceRepository;

    /**
     * Find nearest available slot by location and type
     */
    public Optional<ParkingSpace> findNearestAvailableSlot(String location, SlotType slotType) {
        log.info("Finding nearest available slot at location: {} for type: {}", location, slotType);
        return parkingSpaceRepository.findNearestAvailableSlot(location, slotType);
    }

    /**
     * Find available slot by type only
     */
    public List<ParkingSpace> findAvailableSlotsByType(SlotType slotType) {
        log.info("Finding available slots for type: {}", slotType);
        return parkingSpaceRepository.findAvailableBySlotType(slotType);
    }

    /**
     * Find available slots by location
     */
    public List<ParkingSpace> findAvailableSlotsByLocation(String location) {
        log.info("Finding available slots at location: {}", location);
        return parkingSpaceRepository.findAvailableByLocation(location);
    }

    /**
     * Get all available slots
     */
    public List<ParkingSpace> getAllAvailableSlots() {
        log.info("Fetching all available slots");
        return parkingSpaceRepository.findAvailableParkingSpaces();
    }

    /**
     * Get availability summary
     */
    public AvailabilitySummary getAvailabilitySummary() {
        log.info("Calculating availability summary");
        Long totalAvailable = parkingSpaceRepository.countAllAvailable();
        Long normalAvailable = parkingSpaceRepository.countAvailableBySlotType(SlotType.NORMAL);
        Long vipAvailable = parkingSpaceRepository.countAvailableBySlotType(SlotType.VIP);
        Long handicappedAvailable = parkingSpaceRepository.countAvailableBySlotType(SlotType.HANDICAPPED);

        return AvailabilitySummary.builder()
                .totalAvailableSlots(totalAvailable)
                .normalSlotsAvailable(normalAvailable)
                .vipSlotsAvailable(vipAvailable)
                .handicappedSlotsAvailable(handicappedAvailable)
                .build();
    }

    /**
     * Recommend slot based on vehicle type
     */
    public Optional<ParkingSpace> recommendSlot(String location, String vehicleType) {
        log.info("Recommending slot for vehicle type: {}", vehicleType);
        
        // EV vehicles can use any slot but prefer if EVCharging slot exists (can be extended)
        // For now, all vehicle types get normal slot or first available
        
        Optional<ParkingSpace> nearestSlot = parkingSpaceRepository.findNearestAvailableSlot(location, SlotType.NORMAL);
        
        if (nearestSlot.isEmpty()) {
            log.warn("No slots available at location: {}", location);
        }
        
        return nearestSlot;
    }

    /**
     * Check if slot is available
     */
    public boolean isSlotAvailable(Long parkingSpaceId) {
        Optional<ParkingSpace> slot = parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId);
        return slot.isPresent() && Boolean.TRUE.equals(slot.get().getIsActive())
                && slot.get().getAvailabilityStatus() == SpaceStatus.AVAILABLE;
    }

    /**
     * Get slots by priority
     */
    public List<ParkingSpace> getSlotsByPriority(SlotType slotType, String location) {
        log.info("Getting slots by priority for type: {} at location: {}", slotType, location);
        // Priority: nearest available, then by type
        List<ParkingSpace> slots = parkingSpaceRepository.findNearestAvailableSlots(location, slotType);
        return slots.stream()
                .sorted((s1, s2) -> {
                    // Sort by floor number, then space number
                    int floorCompare = Integer.compare(s1.getFloorNumber() != null ? s1.getFloorNumber() : 0,
                                                     s2.getFloorNumber() != null ? s2.getFloorNumber() : 0);
                    if (floorCompare != 0) return floorCompare;
                    return s1.getSpaceNumber().compareTo(s2.getSpaceNumber());
                })
                .collect(Collectors.toList());
    }

    /**
     * DTO for availability summary
     */
    public static class AvailabilitySummary {
        public Long totalAvailableSlots;
        public Long normalSlotsAvailable;
        public Long vipSlotsAvailable;
        public Long handicappedSlotsAvailable;

        public AvailabilitySummary(Long totalAvailableSlots, Long normalSlotsAvailable, 
                                   Long vipSlotsAvailable, Long handicappedSlotsAvailable) {
            this.totalAvailableSlots = totalAvailableSlots;
            this.normalSlotsAvailable = normalSlotsAvailable;
            this.vipSlotsAvailable = vipSlotsAvailable;
            this.handicappedSlotsAvailable = handicappedSlotsAvailable;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long totalAvailableSlots;
            private Long normalSlotsAvailable;
            private Long vipSlotsAvailable;
            private Long handicappedSlotsAvailable;

            public Builder totalAvailableSlots(Long totalAvailableSlots) {
                this.totalAvailableSlots = totalAvailableSlots;
                return this;
            }

            public Builder normalSlotsAvailable(Long normalSlotsAvailable) {
                this.normalSlotsAvailable = normalSlotsAvailable;
                return this;
            }

            public Builder vipSlotsAvailable(Long vipSlotsAvailable) {
                this.vipSlotsAvailable = vipSlotsAvailable;
                return this;
            }

            public Builder handicappedSlotsAvailable(Long handicappedSlotsAvailable) {
                this.handicappedSlotsAvailable = handicappedSlotsAvailable;
                return this;
            }

            public AvailabilitySummary build() {
                return new AvailabilitySummary(totalAvailableSlots, normalSlotsAvailable, 
                                              vipSlotsAvailable, handicappedSlotsAvailable);
            }
        }

        public Long getTotalAvailableSlots() {
            return totalAvailableSlots;
        }

        public Long getNormalSlotsAvailable() {
            return normalSlotsAvailable;
        }

        public Long getVipSlotsAvailable() {
            return vipSlotsAvailable;
        }

        public Long getHandicappedSlotsAvailable() {
            return handicappedSlotsAvailable;
        }
    }
}
