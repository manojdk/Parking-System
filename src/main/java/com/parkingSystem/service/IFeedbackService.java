package com.parkingSystem.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.parkingSystem.model.Feedback;
import java.util.List;

/**
 * Service interface for managing feedback
 */
public interface IFeedbackService {

    /**
     * Get all Feedback Details
     *
     * @return Feedback data
     */
    Page<Feedback> getAllFeedback(Pageable pageable);

    /**
     * Create new Feedback with parameters
     *
     * @param userId User ID
     * @param reservationId Reservation ID
     * @param rating Rating
     * @param comments Comments
     * @return new Feedback Data
     */
    Feedback createFeedback(Long userId, Long reservationId, Integer rating, String comments);

    /**
     * Update feedback
     *
     * @param feedbackId Feedback ID
     * @param rating Rating
     * @param comments Comments
     * @return Updated feedback
     */
    Feedback updateFeedback(Long feedbackId, Integer rating, String comments);

    /**
     * Delete feedback
     *
     * @param feedbackId Feedback ID
     */
    void deleteFeedback(Long feedbackId);

    /**
     * Get feedback by ID
     *
     * @param feedbackId Feedback ID
     * @return Feedback
     */
    Feedback getFeedbackById(Long feedbackId);

    /**
     * Get feedback by user ID
     *
     * @param userId User ID
     * @return List of feedback
     */
    Page<Feedback> getFeedbacksByUserId(Long userId, Pageable pageable);

    /**
     * Get feedback by reservation ID
     *
     * @param reservationId Reservation ID
     * @return List of feedback
     */
    List<Feedback> getFeedbacksByReservationId(Long reservationId);
}
