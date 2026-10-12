package com.parkingSystem.dto;

import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating a parking space. Null fields are left unchanged.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateParkingSpaceRequest {

    @NotNull(message = "parkingSpaceId is mandatory")
    private Long parkingSpaceId;

    private String location;

    private SlotType type;

    @Positive(message = "rate must be positive")
    private BigDecimal rate;

    /**
     * AVAILABLE or OUT_OF_SERVICE (case-insensitive). RESERVED and OCCUPIED are set by reservations and
     * tickets and are rejected here.
     */
    private SpaceStatus status;
}
