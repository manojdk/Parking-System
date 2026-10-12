package com.parkingSystem.dto;

import com.parkingSystem.enums.SlotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating a parking space. New spaces always start as AVAILABLE.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpaceRequest {

    @NotBlank(message = "spaceNumber is mandatory")
    private String spaceNumber;

    @NotBlank(message = "location is mandatory")
    private String location;

    private Integer floorNumber;

    @NotNull(message = "slotType is mandatory")
    private SlotType slotType;

    @NotNull(message = "ratePerHour is mandatory")
    @Positive(message = "ratePerHour must be positive")
    private BigDecimal ratePerHour;

    private String notes;
}
