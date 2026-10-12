package com.parkingSystem.dto;

import java.util.Map;

import com.parkingSystem.enums.SlotType;

/**
 * Free, in-service spaces right now.
 *
 * @param totalAvailable      all slot types together
 * @param availableBySlotType every slot type, in a fixed order, with 0 where none are free
 */
public record AvailabilitySummaryDTO(long totalAvailable, Map<SlotType, Long> availableBySlotType) {
}
