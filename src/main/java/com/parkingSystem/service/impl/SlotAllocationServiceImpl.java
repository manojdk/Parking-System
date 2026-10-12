package com.parkingSystem.service.impl;

import com.parkingSystem.dto.AvailabilitySummaryDTO;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.enums.VehicleType;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.repository.ParkingSpaceRepository;
import com.parkingSystem.service.ISlotAllocationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only slot lookups.
 */
@Service
@AllArgsConstructor
@Slf4j
public class SlotAllocationServiceImpl implements ISlotAllocationService {

    private ParkingSpaceRepository parkingSpaceRepository;

    public Optional<ParkingSpace> findNearestAvailableSlot(String location, SlotType slotType) {
        log.info("Finding nearest available slot at location: {} for type: {}", location, slotType);
        return parkingSpaceRepository.findNearestAvailableSlot(location, slotType);
    }

    public Page<ParkingSpace> findAvailableSlotsByType(SlotType slotType, Pageable pageable) {
        return parkingSpaceRepository.findAvailableBySlotType(slotType, pageable);
    }

    public Page<ParkingSpace> findAvailableSlotsByLocation(String location, Pageable pageable) {
        return parkingSpaceRepository.findAvailableByLocation(location, pageable);
    }

    public Page<ParkingSpace> getAllAvailableSlots(Pageable pageable) {
        return parkingSpaceRepository.findAvailableParkingSpaces(pageable);
    }

    /**
     * One grouped query instead of one count per type; every slot type is listed, with 0 when none are free.
     */
    public AvailabilitySummaryDTO getAvailabilitySummary() {
        Map<SlotType, Long> counts = new EnumMap<>(SlotType.class);
        for (Object[] row : parkingSpaceRepository.countAvailableGroupedBySlotType()) {
            counts.put((SlotType) row[0], ((Number) row[1]).longValue());
        }
        Map<SlotType, Long> bySlotType = new LinkedHashMap<>();
        long total = 0;
        for (SlotType type : SlotType.values()) {
            long count = counts.getOrDefault(type, 0L);
            bySlotType.put(type, count);
            total += count;
        }
        return new AvailabilitySummaryDTO(total, bySlotType);
    }

    public Optional<ParkingSpace> recommendSlot(String location, VehicleType vehicleType) {
        log.info("Recommending slot for vehicle type: {} at location: {}", vehicleType, location);
        for (SlotType slotType : vehicleType.recommendedSlotTypes()) {
            Optional<ParkingSpace> slot = parkingSpaceRepository.findNearestAvailableSlot(location, slotType);
            if (slot.isPresent()) {
                return slot;
            }
        }
        log.info("No suitable slot for {} at location: {}", vehicleType, location);
        return Optional.empty();
    }

    public boolean isSlotAvailable(Long parkingSpaceId) {
        Optional<ParkingSpace> slot = parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId);
        return slot.isPresent() && Boolean.TRUE.equals(slot.get().getIsActive())
                && slot.get().getAvailabilityStatus() == SpaceStatus.AVAILABLE;
    }

    /**
     * Free slots of a type at a location, lowest floor first, then by space number.
     */
    public List<ParkingSpace> getSlotsByPriority(SlotType slotType, String location) {
        return parkingSpaceRepository.findNearestAvailableSlots(location, slotType).stream()
                .sorted(Comparator
                        .comparing((ParkingSpace s) -> s.getFloorNumber() != null ? s.getFloorNumber() : 0)
                        .thenComparing(ParkingSpace::getSpaceNumber))
                .toList();
    }
}
