package com.parkingSystem.service;

import com.parkingSystem.model.Feedback;
import com.parkingSystem.model.User;
import com.parkingSystem.model.Reservation;
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
    List<Feedback> getAllFeedback();

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
     * @param userId User
     * @return List of feedback
     */
    List<Feedback> getFeedbacksByUserId(User userId);

    /**
     * Get feedback by reservation ID
     *
     * @param reservationId Reservation
     * @return List of feedback
     */
    List<Feedback> getFeedbacksByReservationId(Reservation reservationId);
}
