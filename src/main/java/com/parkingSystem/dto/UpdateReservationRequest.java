package com.parkingSystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a reservation. A null duration keeps the current value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReservationRequest {

    @NotNull(message = "reservationId is mandatory")
    private Long reservationId;

    @NotNull(message = "parkingSpaceId is mandatory")
    private Long parkingSpaceId;

    /** Expected stay in hours (informational, 1-24). */
    @Positive(message = "duration must be positive")
    @Max(value = 24, message = "duration cannot be more than 24 hours")
    private Integer duration;
}
