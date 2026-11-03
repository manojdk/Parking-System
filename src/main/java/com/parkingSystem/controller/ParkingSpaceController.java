package com.parkingSystem.controller;

import com.parkingSystem.dto.ParkingSpaceDTO;
import com.parkingSystem.dto.PageResponse;
import com.parkingSystem.controller.support.Pagination;
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
	public PageResponse<ParkingSpaceDTO> getAllParkingSpaces(
			@RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
		return PageResponse.of(parkingSpaceService.getAllParkingSpaces(
				Pagination.of(page, size, SlotAllocationController.BY_FLOOR_AND_NUMBER)), ParkingSpaceDTO::from);
	}

	@GetMapping("/details/id")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ParkingSpaceDTO> getParkingSpaceDetails(@RequestParam Long parkingSpaceId) {
		return ResponseEntity.ok(ParkingSpaceDTO.from(parkingSpaceService.getParkingSpaceDetails(parkingSpaceId)));
	}

	@PostMapping("/register")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> addParkingSpace(@Valid @RequestBody ParkingSpaceRequest request) {
		ParkingSpace addedParkingSpace = parkingSpaceService.addParkingSpace(toEntity(request));
		return ResponseEntity.status(HttpStatus.CREATED).body(ParkingSpaceDTO.from(addedParkingSpace));
	}

	@PostMapping("/register/multiple")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> addMultipleParkingSpaces(
			@RequestBody List<@Valid ParkingSpaceRequest> parkingSpaceRequests) {
		List<ParkingSpace> addedParkingSpaces = parkingSpaceService
				.addMultipleParkingSpaces(parkingSpaceRequests.stream().map(ParkingSpaceController::toEntity).toList());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(addedParkingSpaces.stream().map(ParkingSpaceDTO::from).toList());
	}

	@PutMapping("/update")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> updateParkingSpace(@Valid @RequestBody UpdateParkingSpaceRequest request) {
		ParkingSpace updatedParkingSpace = parkingSpaceService.updateParkingSpace(request.getParkingSpaceId(),
				request.getLocation(), request.getType(), request.getRate(), request.getStatus());
		return ResponseEntity.ok(ParkingSpaceDTO.from(updatedParkingSpace));
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
	public PageResponse<ParkingSpaceDTO> getAvailableParkingSpaces(
			@RequestParam(defaultValue = "AVAILABLE") String availabilityStatus,
			@RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
		return PageResponse.of(parkingSpaceService.getAvailableParkingSpaces(SpaceStatus.fromValue(availabilityStatus),
				Pagination.of(page, size, SlotAllocationController.BY_FLOOR_AND_NUMBER)), ParkingSpaceDTO::from);
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
