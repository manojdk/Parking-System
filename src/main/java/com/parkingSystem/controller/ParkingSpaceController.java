package com.parkingSystem.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parkingSystem.dto.ParkingSpaceRequest;
import com.parkingSystem.dto.UpdateParkingSpaceRequest;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.service.IParkingSpaceService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

/**
 * Parking space management. Everything is admin-only except the public availability lookup.
 */
@RestController
@RequestMapping("/api/parkingspace")
@AllArgsConstructor
public class ParkingSpaceController {

	private IParkingSpaceService parkingSpaceService;

	@GetMapping(value = "/all/details")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> getAllParkingSpaces() {
		List<ParkingSpace> parkingSpaces = parkingSpaceService.getAllParkingSpaces();
		return ResponseEntity.ok(parkingSpaces);
	}

	@GetMapping("/details/id")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> getParkingSpaceDetails(@RequestParam Long parkingSpaceId) {
		Map<String, Object> parkingSpaceDetails = parkingSpaceService.getParkingSpaceDetails(parkingSpaceId);
		return ResponseEntity.ok(parkingSpaceDetails);
	}

	@PostMapping("/register")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> addParkingSpace(@Valid @RequestBody ParkingSpaceRequest request) {
		ParkingSpace addedParkingSpace = parkingSpaceService.addParkingSpace(toEntity(request));
		return ResponseEntity.status(HttpStatus.CREATED).body(addedParkingSpace);
	}

	@PostMapping("/register/multiple")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> addMultipleParkingSpaces(
			@RequestBody List<@Valid ParkingSpaceRequest> parkingSpaceRequests) {
		List<ParkingSpace> addedParkingSpaces = parkingSpaceService
				.addMultipleParkingSpaces(parkingSpaceRequests.stream().map(ParkingSpaceController::toEntity).toList());
		return ResponseEntity.status(HttpStatus.CREATED).body(addedParkingSpaces);
	}

	@PutMapping("/update")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> updateParkingSpace(@Valid @RequestBody UpdateParkingSpaceRequest request) {
		ParkingSpace updatedParkingSpace = parkingSpaceService.updateParkingSpace(request.getParkingSpaceId(),
				request.getLocation(), request.getType(), request.getRate(), request.getStatus());
		return ResponseEntity.ok(updatedParkingSpace);
	}

	@DeleteMapping("/remove")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> deleteParkingSpaceById(@RequestParam Long parkingSpaceId) {
		parkingSpaceService.deleteParkingSpace(parkingSpaceId);
		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Parking space removed successfully");
		return ResponseEntity.ok(response);
	}

	/**
	 * Public: lists in-service parking spaces by status (AVAILABLE by default; case-insensitive).
	 */
	@GetMapping("/available")
	public ResponseEntity<Object> getAvailableParkingSpaces(
			@RequestParam(defaultValue = "AVAILABLE") String availabilityStatus) {
		List<ParkingSpace> availableParkingSpaces = parkingSpaceService
				.getAvailableParkingSpaces(SpaceStatus.fromValue(availabilityStatus));
		return ResponseEntity.ok(availableParkingSpaces);
	}

	private static ParkingSpace toEntity(ParkingSpaceRequest request) {
		return ParkingSpace.builder()
				.spaceNumber(request.getSpaceNumber())
				.location(request.getLocation())
				.floorNumber(request.getFloorNumber())
				.slotType(request.getSlotType())
				.ratePerHour(request.getRatePerHour())
				.notes(request.getNotes())
				.build();
	}
}
