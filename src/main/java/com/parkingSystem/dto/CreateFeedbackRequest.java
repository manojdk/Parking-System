package com.parkingSystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating feedback. If userId is omitted the authenticated user is used.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFeedbackRequest {

    private Long userId;

    @NotNull(message = "reservationId is mandatory")
    private Long reservationId;

    @NotNull(message = "rating is mandatory")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 255, message = "comments cannot be longer than 255 characters")
    private String comments;
}
