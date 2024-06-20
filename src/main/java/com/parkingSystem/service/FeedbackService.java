package com.parkingSystem.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.parkingSystem.model.Feedback;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.repository.FeedbackRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FeedbackService {

	private FeedbackRepository feedbackRepository;

	private UserService userService;

	private ReservationService reservationService;

	@Transactional
	public Feedback addFeedback(Long userId, Long reservationId, Integer rating, String comments) {
		try {
			// Retrieve the user and reservation
			User user = userService.getUserById(userId)
					.orElseThrow(() -> new IllegalArgumentException("User not found."));
			Reservation reservation = reservationService.getReservationById(reservationId);

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

	public Optional<Feedback> getFeedbackById(Long feedbackId) {
		try {
			return feedbackRepository.findByFeedbackId(feedbackId);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching feedback by ID: " + e.getMessage());
		}
	}

	public List<Feedback> getFeedbacksByUserId(User userId) {
		try {
			return feedbackRepository.findAllByUser(userId);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching feedbacks by user ID: " + e.getMessage());
		}
	}

	public List<Feedback> getFeedbacksByReservationId(Reservation reservationId) {
		try {
			return feedbackRepository.findAllByReservation(reservationId);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching feedbacks by reservation ID: " + e.getMessage());
		}
	}
}
