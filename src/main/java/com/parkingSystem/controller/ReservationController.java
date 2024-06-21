package com.parkingSystem.controller;

import java.time.LocalDateTime;
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

import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.service.ReservationService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/reservation")
@AllArgsConstructor
public class ReservationController {

	private ReservationService reservationService;

	@GetMapping("/all/details")
	public ResponseEntity<Object> getAllReservations() {
		try {
			List<Reservation> reservations = reservationService.getAllReservations();
			return ResponseEntity.ok(reservations);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching reservations: " + e.getMessage());
		}
	}

	@PostMapping("/register")
	public ResponseEntity<Object> createReservation(@RequestBody Map<String, String> request) {
		try {
			Long userId = Long.parseLong(request.get("userId"));
			Long parkingSpaceId = Long.parseLong(request.get("parkingSpaceId"));
			LocalDateTime reservationTime = LocalDateTime.parse(request.get("reservationTime"));
			Integer duration = Integer.parseInt(request.get("duration"));

			Reservation reservation = reservationService.createReservation(userId, parkingSpaceId, reservationTime,
					duration);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Reservation successful");
			response.put("reservationId", reservation.getReservationId());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error making reservation: " + e.getMessage());
		}
	}

	@GetMapping("/details")
	public ResponseEntity<Object> getReservationDetails(@RequestParam Long reservationId) {
		try {
			Reservation reservation = reservationService.getReservationById(reservationId);
			Map<String, Object> response = new HashMap<>();
			response.put("reservationId", reservation.getReservationId());
			response.put("userId", reservation.getUser().getUserId());
			response.put("parkingSpaceId", reservation.getParkingSpace().getParkingSpaceId());
			response.put("reservationTime", reservation.getReservationTime());
			response.put("duration", reservation.getDuration());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving reservation details: " + e.getMessage());
		}
	}

	@GetMapping("/user")
	public ResponseEntity<Object> getReservationsByUserId(@RequestParam User userId) {
		try {
			List<Reservation> reservations = reservationService.getReservationsByUserId(userId);
			return ResponseEntity.ok(reservations);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving reservations: " + e.getMessage());
		}
	}

	@PutMapping("/update")
	public ResponseEntity<Object> updateReservation(@RequestBody Map<String, String> request) {
		try {
			Long reservationId = Long.parseLong(request.get("reservationId"));
			LocalDateTime reservationTime = request.containsKey("reservationTime")
					? LocalDateTime.parse(request.get("reservationTime"))
					: null;
			Integer duration = request.containsKey("duration") ? Integer.parseInt(request.get("duration")) : null;

			Reservation updatedReservation = reservationService.updateReservation(reservationId, reservationTime,
					reservationId, duration);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Reservation updated successfully");
			response.put("reservationId", updatedReservation.getReservationId());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error updating reservation: " + e.getMessage());
		}
	}

	@DeleteMapping("/remove")
	public ResponseEntity<Object> deleteReservation(@RequestParam Long reservationId) {
		try {
			reservationService.deleteReservation(reservationId);
			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Reservation deleted successfully");
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error deleting reservation: " + e.getMessage());
		}
	}
}
