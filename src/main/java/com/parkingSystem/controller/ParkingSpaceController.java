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

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.service.ParkingSpaceService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/parkingspace")
@AllArgsConstructor
public class ParkingSpaceController {

	private ParkingSpaceService parkingSpaceService;

	@GetMapping(value = "/details")
	public ResponseEntity<Object> getAllParkingSpaces() {
		try {
			List<ParkingSpace> parkingSpaces = parkingSpaceService.getAllParkingSpaces();
			return ResponseEntity.ok(parkingSpaces);
		} catch (Exception e) {
			// return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
			e.getMessage();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/available")
	public ResponseEntity<Object> getAvailableParkingSpaces() {
		try {
			List<ParkingSpace> parkingSpaces = parkingSpaceService.getAvailableParkingSpaces(null);
			return ResponseEntity.ok(parkingSpaces);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching available parking spaces: " + e.getMessage());
		}
	}

	@GetMapping("/details/id")
	public ResponseEntity<Object> getParkingSpaceDetails(@RequestParam Long parkingSpaceId) {
		try {
			Map<String, Object> parkingSpaceDetails = parkingSpaceService.getParkingSpaceDetails(parkingSpaceId);
			return ResponseEntity.ok(parkingSpaceDetails);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching parking space details: " + e.getMessage());
		}
	}

	@PostMapping("/register")
	public ResponseEntity<Object> addParkingSpace(@RequestBody Map<String, String> request) {
		try {
			String location = request.get("location");
			String type = request.get("type");
			Double rate = Double.parseDouble(request.get("rate"));

			ParkingSpace addedParkingSpace = parkingSpaceService.addParkingSpace(location, type, rate);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Parking space added successfully");
			response.put("parkingSpaceId", addedParkingSpace.getParkingSpaceId());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error adding parking space: " + e.getMessage());
		}
	}

	@PutMapping("/update")
	public ResponseEntity<Object> updateParkingSpace(@RequestBody Map<String, Object> request) {
		try {
			Long parkingSpaceId = Long.parseLong(request.get("parkingSpaceId").toString());
			String location = (String) request.get("location");
			String type = (String) request.get("type");
			Double rate = request.containsKey("rate") ? Double.parseDouble(request.get("rate").toString()) : null;
			String availabilityStatus = (String) request.get("availabilityStatus");

			ParkingSpace updatedParkingSpace = parkingSpaceService.updateParkingSpace(parkingSpaceId, location, type,
					rate, availabilityStatus);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Parking space updated successfully");
			response.put("parkingSpaceId", updatedParkingSpace.getParkingSpaceId());

			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error updating parking space: " + e.getMessage());
		}
	}

	@DeleteMapping("/remove")
	public ResponseEntity<Object> deleteParkingSpaceById(@RequestParam Long parkingSpaceId) {
		try {
			parkingSpaceService.deleteParkingSpace(parkingSpaceId);
			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Parking space deleted successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Parking space not found with ID: " + parkingSpaceId);
		}
	}
}
