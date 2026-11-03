package com.parkingSystem.service.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.util.List;

import org.springframework.stereotype.Service;

import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.enums.ReservationStatus;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.Feedback;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.repository.FeedbackRepository;
import com.parkingSystem.repository.ReservationRepository;
import com.parkingSystem.repository.UserRepository;
import com.parkingSystem.service.IFeedbackService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FeedbackServiceImpl implements IFeedbackService {

	private FeedbackRepository feedbackRepository;

	private UserRepository userRepository;

	private ReservationRepository reservationRepository;

	/**
	 * Get all Feedback Details
	 * 
	 * @return Feedback data
	 */
	public Page<Feedback> getAllFeedback(Pageable pageable) {
		return feedbackRepository.findAll(pageable);
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
		// Retrieve the user and reservation
		User user = userRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
		Reservation reservation = getReservation(reservationId);
		if (!user.getUserId().equals(reservation.getUser().getUserId())) {
			throw new BadRequestException("Reservation does not belong to this user");
		}
		if (reservation.getStatus() != ReservationStatus.FULFILLED) {
			throw new BadRequestException("Feedback can only be given for a reservation that was used. Current status: "
					+ reservation.getStatus());
		}
		if (feedbackRepository.existsByReservationReservationId(reservationId)) {
			throw new ConflictException("Feedback already exists for this reservation; update it instead");
		}

		// Create new feedback
		Feedback feedback = new Feedback();
		feedback.setUser(user);
		feedback.setReservation(reservation);
		feedback.setRating(rating);
		feedback.setComments(comments);

		// Save feedback
		return feedbackRepository.save(feedback);
	}

	/**
	 * Get Feedback details by feedbackId
	 * 
	 * @param feedbackId
	 * @return Feedback data
	 */
	public Feedback getFeedbackById(Long feedbackId) {
		Feedback feedback = feedbackRepository.findByFeedbackId(feedbackId);
		if (feedback == null) {
			throw new ResourceNotFoundException("Feedback not found with ID: " + feedbackId);
		}
		return feedback;
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
		Feedback feedback = getFeedbackById(feedbackId);

		if (rating != null) {
			feedback.setRating(rating);
		}
		if (comments != null) {
			feedback.setComments(comments);
		}

		return feedbackRepository.save(feedback);
	}

	/**
	 * Get Feedback details by userId
	 * 
	 * @param userId
	 * @return Feedback Data
	 */
	public Page<Feedback> getFeedbacksByUserId(Long userId, Pageable pageable) {
		return feedbackRepository.findAllByUserUserId(userId, pageable);
	}

	/**
	 * Get Feedback details by reservationId
	 * 
	 * @param reservationId
	 * @return Feedback Data
	 */
	public List<Feedback> getFeedbacksByReservationId(Long reservationId) {
		return feedbackRepository.findAllByReservation(getReservation(reservationId));
	}

	/**
	 * Delete Feedback by FeedbackId
	 * 
	 * @param feedbackId
	 */
	@Transactional
	public void deleteFeedback(Long feedbackId) {
		getFeedbackById(feedbackId);
		feedbackRepository.deleteByFeedbackId(feedbackId);
	}

	private Reservation getReservation(Long reservationId) {
		Reservation reservation = reservationRepository.findByReservationId(reservationId);
		if (reservation == null) {
			throw new ResourceNotFoundException("Reservation not found with ID: " + reservationId);
		}
		return reservation;
	}
}
