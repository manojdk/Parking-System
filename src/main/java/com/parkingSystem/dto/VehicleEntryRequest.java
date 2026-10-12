package com.parkingSystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Vehicle Entry request. If userId is omitted the authenticated user is used.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleEntryRequest {
    private Long userId;
    @NotNull(message = "vehicleId is mandatory")
    private Long vehicleId;
    @NotNull(message = "parkingSpaceId is mandatory")
    private Long parkingSpaceId;
}

