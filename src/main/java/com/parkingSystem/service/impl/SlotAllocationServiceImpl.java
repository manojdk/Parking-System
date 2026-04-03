package com.parkingSystem.service.impl;

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.uuidTest.ParkingSpaceRepository;
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
        try {
            log.info("Finding nearest available slot at location: {} for type: {}", location, slotType);
            return parkingSpaceRepository.findNearestAvailableSlot(location, slotType);
        } catch (Exception e) {
            log.error("Error finding nearest slot: {}", e.getMessage());
            throw new RuntimeException("Error finding slot: " + e.getMessage());
        }
    }

    /**
     * Find available slot by type only
     */
    public List<ParkingSpace> findAvailableSlotsByType(SlotType slotType) {
        try {
            log.info("Finding available slots for type: {}", slotType);
            return parkingSpaceRepository.findAvailableBySlotType(slotType);
        } catch (Exception e) {
            log.error("Error finding slots by type: {}", e.getMessage());
            throw new RuntimeException("Error finding slots: " + e.getMessage());
        }
    }

    /**
     * Find available slots by location
     */
    public List<ParkingSpace> findAvailableSlotsByLocation(String location) {
        try {
            log.info("Finding available slots at location: {}", location);
            return parkingSpaceRepository.findAvailableByLocation(location);
        } catch (Exception e) {
            log.error("Error finding slots by location: {}", e.getMessage());
            throw new RuntimeException("Error finding slots: " + e.getMessage());
        }
    }

    /**
     * Get all available slots
     */
    public List<ParkingSpace> getAllAvailableSlots() {
        try {
            log.info("Fetching all available slots");
            return parkingSpaceRepository.findAvailableParkingSpaces();
        } catch (Exception e) {
            log.error("Error fetching available slots: {}", e.getMessage());
            throw new RuntimeException("Error fetching slots: " + e.getMessage());
        }
    }

    /**
     * Get availability summary
     */
    public AvailabilitySummary getAvailabilitySummary() {
        try {
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
        } catch (Exception e) {
            log.error("Error calculating availability: {}", e.getMessage());
            throw new RuntimeException("Error calculating availability: " + e.getMessage());
        }
    }

    /**
     * Recommend slot based on vehicle type
     */
    public Optional<ParkingSpace> recommendSlot(String location, String vehicleType) {
        try {
            log.info("Recommending slot for vehicle type: {}", vehicleType);
            
            // EV vehicles can use any slot but prefer if EVCharging slot exists (can be extended)
            // For now, all vehicle types get normal slot or first available
            
            Optional<ParkingSpace> nearestSlot = parkingSpaceRepository.findNearestAvailableSlot(location, SlotType.NORMAL);
            
            if (nearestSlot.isEmpty()) {
                log.warn("No slots available at location: {}", location);
            }
            
            return nearestSlot;
        } catch (Exception e) {
            log.error("Error recommending slot: {}", e.getMessage());
            throw new RuntimeException("Error recommending slot: " + e.getMessage());
        }
    }

    /**
     * Check if slot is available
     */
    public boolean isSlotAvailable(Long parkingSpaceId) {
        try {
            Optional<ParkingSpace> slot = parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId);
            return slot.isPresent() && "available".equalsIgnoreCase(slot.get().getAvailabilityStatus());
        } catch (Exception e) {
            log.error("Error checking slot availability: {}", e.getMessage());
            throw new RuntimeException("Error checking slot: " + e.getMessage());
        }
    }

    /**
     * Allocate slot
     */
    public ParkingSpace allocateSlot(Long parkingSpaceId) {
        try {
            log.info("Allocating slot ID: {}", parkingSpaceId);
            ParkingSpace slot = parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId)
                    .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
            
            if (!"available".equalsIgnoreCase(slot.getAvailabilityStatus())) {
                throw new IllegalArgumentException("Slot is not available");
            }
            
            slot.setAvailabilityStatus("occupied");
            return parkingSpaceRepository.save(slot);
        } catch (Exception e) {
            log.error("Error allocating slot: {}", e.getMessage());
            throw new RuntimeException("Error allocating slot: " + e.getMessage());
        }
    }

    /**
     * Release slot
     */
    public void releaseSlot(Long parkingSpaceId) {
        try {
            log.info("Releasing slot ID: {}", parkingSpaceId);
            ParkingSpace slot = parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId)
                    .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
            
            slot.setAvailabilityStatus("available");
            parkingSpaceRepository.save(slot);
        } catch (Exception e) {
            log.error("Error releasing slot: {}", e.getMessage());
            throw new RuntimeException("Error releasing slot: " + e.getMessage());
        }
    }

    /**
     * Get slots by priority
     */
    public List<ParkingSpace> getSlotsByPriority(SlotType slotType, String location) {
        try {
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
        } catch (Exception e) {
            log.error("Error getting slots by priority: {}", e.getMessage());
            throw new RuntimeException("Error getting slots: " + e.getMessage());
        }
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
