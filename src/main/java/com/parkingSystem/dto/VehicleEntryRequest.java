package com.parkingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Vehicle Entry request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleEntryRequest {
    private Long userId;
    private Long vehicleId;
    private Long parkingSpaceId;
}

