package com.parkingSystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.model.ParkingSpace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parking space response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpaceDTO {
    private Long parkingSpaceId;
    private String spaceNumber;
    private String location;
    private Integer floorNumber;
    private SlotType slotType;
    private SpaceStatus availabilityStatus;
    private BigDecimal ratePerHour;
    private String notes;
    /** false = removed by an admin (kept for history). */
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ParkingSpaceDTO from(ParkingSpace space) {
        return ParkingSpaceDTO.builder()
                .parkingSpaceId(space.getParkingSpaceId())
                .spaceNumber(space.getSpaceNumber())
                .location(space.getLocation())
                .floorNumber(space.getFloorNumber())
                .slotType(space.getSlotType())
                .availabilityStatus(space.getAvailabilityStatus())
                .ratePerHour(space.getRatePerHour())
                .notes(space.getNotes())
                .isActive(space.getIsActive())
                .createdAt(space.getCreatedAt())
                .updatedAt(space.getUpdatedAt())
                .build();
    }
}
