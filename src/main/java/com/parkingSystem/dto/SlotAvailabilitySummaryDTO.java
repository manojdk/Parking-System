package com.parkingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Slot Availability Summary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotAvailabilitySummaryDTO {
    private Long totalAvailableSlots;
    private Long normalSlotsAvailable;
    private Long vipSlotsAvailable;
    private Long handicappedSlotsAvailable;
    private Long totalOccupied;
    private Double occupancyPercentage;
}

