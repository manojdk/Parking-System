package com.parkingSystem.controller;

import com.parkingSystem.dto.CreateReservationRequest;
import com.parkingSystem.dto.ReservationDTO;
import com.parkingSystem.dto.UpdateReservationRequest;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.security.CurrentUserService;
import com.parkingSystem.service.IReservationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservation")
@AllArgsConstructor
public class ReservationController {

	private IReservationService reservationService;

	private CurrentUserService currentUserService;

	@GetMapping("/all/details")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> getAllReservations() {
		List<ReservationDTO> reservations = reservationService.getAllReservations().stream()
				.map(ReservationDTO::from).toList();
		return ResponseEntity.ok(reservations);
	}

	@PostMapping("/register")
	public ResponseEntity<Object> createReservation(@Valid @RequestBody CreateReservationRequest request) {
		Long userId = currentUserService.resolveUserId(request.getUserId());

		Reservation createdReservation = reservationService.createReservation(userId, request.getParkingSpaceId(),
				request.getDuration());
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("status", "success");
		response.put("message", "Reservation created successfully");
		response.put("reservation", ReservationDTO.from(createdReservation));

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/details")
	public ResponseEntity<Object> getReservationDetails(@RequestParam Long reservationId) {
		Reservation reservation = reservationService.getReservationById(reservationId);
		currentUserService.checkAccess(reservation.getUser().getUserId());
		return ResponseEntity.ok(ReservationDTO.from(reservation));
	}

	@GetMapping("/user")
	public ResponseEntity<Object> getReservationsByUserId(@RequestParam Long userId) {
		currentUserService.checkAccess(userId);
		List<ReservationDTO> reservations = reservationService.getReservationsByUserId(userId).stream()
				.map(ReservationDTO::from).toList();
		return ResponseEntity.ok(reservations);
	}

	@PutMapping("/update")
	public ResponseEntity<Object> updateReservation(@Valid @RequestBody UpdateReservationRequest request) {
		Reservation existing = reservationService.getReservationById(request.getReservationId());
		currentUserService.checkAccess(existing.getUser().getUserId());

		Reservation updatedReservation = reservationService.updateReservation(request.getReservationId(),
				request.getParkingSpaceId(), request.getDuration());

		return ResponseEntity.ok(ReservationDTO.from(updatedReservation));
	}

	/**
	 * Cancels the reservation and frees its space. Reservations are never deleted (feedback and history
	 * refer to them); this endpoint keeps its old path and now returns the CANCELLED reservation.
	 */
	@DeleteMapping("/remove")
	public ResponseEntity<Object> deleteReservation(@RequestParam Long reservationId) {
		Reservation existing = reservationService.getReservationById(reservationId);
		currentUserService.checkAccess(existing.getUser().getUserId());

		Reservation cancelled = reservationService.cancelReservation(reservationId);
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("status", "success");
		response.put("message", "Reservation cancelled successfully");
		response.put("reservation", ReservationDTO.from(cancelled));
		return ResponseEntity.ok(response);
	}
}
