package com.parkingSystem.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parkingSystem.model.Feedback;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.service.FeedbackService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/feedback")
@AllArgsConstructor
public class FeedbackController {

	private FeedbackService feedbackService;

	@GetMapping("/all/details")
	public ResponseEntity<Object> getAllFeedback() {
		try {
			List<Feedback> feedBacks = feedbackService.getAllFeedback();
			return ResponseEntity.ok(feedBacks);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching feedback: " + e.getMessage());
		}
	}

	@GetMapping("/details")
	public ResponseEntity<Object> getFeedbackDetails(@RequestParam Long feedbackId) {
		try {
			Feedback feedback = feedbackService.getFeedbackById(feedbackId);
			Map<String, Object> response = new HashMap<>();
			response.put("feedbackId", feedback.getFeedbackId());
			response.put("userId", feedback.getUser().getUserId());
			response.put("reservationId", feedback.getReservation().getReservationId());
			response.put("rating", feedback.getRating());
			response.put("comments", feedback.getComments());
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching feedback details: " + e.getMessage());
		}
	}

	@PostMapping("/register")
	public ResponseEntity<Object> createFeedback(@RequestBody Map<String, String> request) {
		try {
			Long userId = Long.parseLong(request.get("userId"));
			Long reservationId = Long.parseLong(request.get("reservationId"));
			Integer rating = Integer.parseInt(request.get("rating"));
			String comments = request.get("comments");

			Feedback feedback = feedbackService.createFeedback(userId, reservationId, rating, comments);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Feedback created successfully");
			response.put("feedbackId", feedback.getFeedbackId());
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error creating feedback: " + e.getMessage());
		}
	}

	@PutMapping("/update")
	public ResponseEntity<Object> updateFeedback(@RequestBody Map<String, String> request) {
		try {
			Long feedbackId = Long.parseLong(request.get("feedbackId"));
			Integer rating = Integer.parseInt(request.get("rating"));
			String comments = request.get("comments");

			Feedback feedback = feedbackService.updateFeedback(feedbackId, rating, comments);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Feedback updated successfully");
			response.put("feedbackId", feedback.getFeedbackId());

			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error updating feedback: " + e.getMessage());
		}
	}

	@DeleteMapping("/delete")
	public ResponseEntity<Object> deleteFeedback(@RequestParam Long feedbackId) {
		try {
			feedbackService.deleteFeedback(feedbackId);
			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Feedback deleted successfully");
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error deleting feedback: " + e.getMessage());
		}
	}

	@GetMapping("/details/user")
	public ResponseEntity<Object> getFeedbacksByUserId(@RequestParam User userId) {
		try {
			List<Feedback> feedbacks = feedbackService.getFeedbacksByUserId(userId);
			return ResponseEntity.ok(feedbacks);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching feedback by user ID: " + e.getMessage());
		}
	}

	@GetMapping("/details/reservation")
	public ResponseEntity<Object> getFeedbacksByReservationId(@RequestParam Reservation reservationId) {
		try {
			List<Feedback> feedbacks = feedbackService.getFeedbacksByReservationId(reservationId);
			return ResponseEntity.ok(feedbacks);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching feedback by reservation ID: " + e.getMessage());
		}
	}
}
