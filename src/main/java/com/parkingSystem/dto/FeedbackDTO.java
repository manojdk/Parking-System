package com.parkingSystem.dto;

import com.parkingSystem.model.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Feedback response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackDTO {
    private Long feedbackId;
    private Long userId;
    private Long reservationId;
    private Integer rating;
    private String comments;

    public static FeedbackDTO from(Feedback feedback) {
        return FeedbackDTO.builder()
                .feedbackId(feedback.getFeedbackId())
                .userId(feedback.getUser().getUserId())
                .reservationId(feedback.getReservation().getReservationId())
                .rating(feedback.getRating())
                .comments(feedback.getComments())
                .build();
    }
}
