package com.parkingSystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a reservation. If userId is omitted the authenticated user is used.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationRequest {

    private Long userId;

    @NotNull(message = "parkingSpaceId is mandatory")
    private Long parkingSpaceId;

    @NotNull(message = "duration is mandatory")
    /** Expected stay in hours (informational, 1-24). */
    @Positive(message = "duration must be positive")
    @Max(value = 24, message = "duration cannot be more than 24 hours")
    private Integer duration;
}
