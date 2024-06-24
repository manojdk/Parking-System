package com.parkingSystem.service;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.parkingSystem.model.Feedback;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.repository.FeedbackRepository;
import com.parkingSystem.repository.ReservationRepository;
import com.parkingSystem.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FeedbackService {

	private FeedbackRepository feedbackRepository;

	private UserRepository userRepository;

	private ReservationRepository reservationRepository;

	/**
	 * Get all Feedback Details
	 * 
	 * @return Feedback data
	 */
	public List<Feedback> getAllFeedback() {
		try {
			return feedbackRepository.findAll();
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Error occurred while fetching feedback data: " + e.getMostSpecificCause().getMessage());
		}
	}

	/**
	 * Create new Feedback with parameters
	 * 
	 * @param userId
	 * @param reservationId
	 * @param rating
	 * @param comments
	 * @return new Feedback Data
	 */
	@Transactional
	public Feedback createFeedback(Long userId, Long reservationId, Integer rating, String comments) {
		try {
			// Retrieve the user and reservation
			User user = userRepository.findByUserId(userId)
					.orElseThrow(() -> new IllegalArgumentException("User not found."));
			Reservation reservation = reservationRepository.findByReservationId(reservationId);

			// Create new feedback
			Feedback feedback = new Feedback();
			feedback.setUser(user);
			feedback.setReservation(reservation);
			feedback.setRating(rating);
			feedback.setComments(comments);

			// Save feedback
			return feedbackRepository.save(feedback);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while adding feedback: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/**
	 * Get Feedback details by feedbackId
	 * 
	 * @param feedbackId
	 * @return Feedback data
	 */
	public Feedback getFeedbackById(Long feedbackId) {
		try {
			return feedbackRepository.findByFeedbackId(feedbackId);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching feedback by ID: " + e.getMessage());
		}
	}

	/**
	 * Update Feedback details
	 * 
	 * @param feedbackId
	 * @param rating
	 * @param comments
	 * @return Updated Feedback data
	 */
	@Transactional
	public Feedback updateFeedback(Long feedbackId, Integer rating, String comments) {
		try {
			Feedback feedback = feedbackRepository.findByFeedbackId(feedbackId);

			if (rating != null) {
				feedback.setRating(rating);
			}
			if (comments != null) {
				feedback.setComments(comments);
			}

			return feedbackRepository.save(feedback);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while updating feedback: " + e.getMostSpecificCause().getMessage());
		}
	}

	/**
	 * Get Feedback details by userId
	 * 
	 * @param userId
	 * @return Feedback Data
	 */
	public List<Feedback> getFeedbacksByUserId(User userId) {
		try {
			return feedbackRepository.findAllByUser(userId);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching feedbacks by user ID: " + e.getMessage());
		}
	}

	/**
	 * Get Feedback details by reservationId
	 * 
	 * @param reservationId
	 * @return Feedback Data
	 */
	public List<Feedback> getFeedbacksByReservationId(Reservation reservationId) {
		try {
			return feedbackRepository.findAllByReservation(reservationId);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching feedbacks by reservation ID: " + e.getMessage());
		}
	}

	/**
	 * Delete Feedback by FeedbackId
	 * 
	 * @param feedbackId
	 */
	@Transactional
	public void deleteFeedback(Long feedbackId) {
		try {
			feedbackRepository.deleteByFeedbackId(feedbackId);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while deleting feedback: " + e.getMostSpecificCause().getMessage());
		}
	}
}
