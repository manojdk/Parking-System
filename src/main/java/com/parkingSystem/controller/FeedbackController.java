package com.parkingSystem.controller;

import com.parkingSystem.dto.CreateFeedbackRequest;
import com.parkingSystem.dto.FeedbackDTO;
import com.parkingSystem.dto.UpdateFeedbackRequest;
import com.parkingSystem.model.Feedback;
import com.parkingSystem.security.CurrentUserService;
import com.parkingSystem.service.IFeedbackService;
import com.parkingSystem.service.IReservationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@AllArgsConstructor
public class FeedbackController {

	private IFeedbackService feedbackService;

	private IReservationService reservationService;

	private CurrentUserService currentUserService;

	@GetMapping("/all/details")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> getAllFeedback() {
		List<FeedbackDTO> feedBacks = feedbackService.getAllFeedback().stream().map(FeedbackDTO::from).toList();
		return ResponseEntity.ok(feedBacks);
	}

	@GetMapping("/details")
	public ResponseEntity<Object> getFeedbackDetails(@RequestParam Long feedbackId) {
		Feedback feedback = feedbackService.getFeedbackById(feedbackId);
		currentUserService.checkAccess(feedback.getUser().getUserId());
		return ResponseEntity.ok(FeedbackDTO.from(feedback));
	}

	@PostMapping("/register")
	public ResponseEntity<Object> createFeedback(@Valid @RequestBody CreateFeedbackRequest request) {
		Long userId = currentUserService.resolveUserId(request.getUserId());

		Feedback feedback = feedbackService.createFeedback(userId, request.getReservationId(), request.getRating(),
				request.getComments());

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Feedback created successfully");
		response.put("feedbackId", feedback.getFeedbackId());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/update")
	public ResponseEntity<Object> updateFeedback(@Valid @RequestBody UpdateFeedbackRequest request) {
		Feedback existing = feedbackService.getFeedbackById(request.getFeedbackId());
		currentUserService.checkAccess(existing.getUser().getUserId());

		Feedback feedback = feedbackService.updateFeedback(request.getFeedbackId(), request.getRating(),
				request.getComments());

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Feedback updated successfully");
		response.put("feedbackId", feedback.getFeedbackId());

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/delete")
	public ResponseEntity<Object> deleteFeedback(@RequestParam Long feedbackId) {
		Feedback existing = feedbackService.getFeedbackById(feedbackId);
		currentUserService.checkAccess(existing.getUser().getUserId());

		feedbackService.deleteFeedback(feedbackId);
		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Feedback deleted successfully");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/details/user")
	public ResponseEntity<Object> getFeedbacksByUserId(@RequestParam Long userId) {
		currentUserService.checkAccess(userId);
		List<FeedbackDTO> feedbacks = feedbackService.getFeedbacksByUserId(userId).stream()
				.map(FeedbackDTO::from).toList();
		return ResponseEntity.ok(feedbacks);
	}

	@GetMapping("/details/reservation")
	public ResponseEntity<Object> getFeedbacksByReservationId(@RequestParam Long reservationId) {
		currentUserService.checkAccess(reservationService.getReservationById(reservationId).getUser().getUserId());
		List<FeedbackDTO> feedbacks = feedbackService.getFeedbacksByReservationId(reservationId).stream()
				.map(FeedbackDTO::from).toList();
		return ResponseEntity.ok(feedbacks);
	}
}
