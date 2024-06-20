package com.parkingSystem.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
	public ResponseEntity<List<ParkingSpace>> getAllParkingSpaces() {
		try {
			List<ParkingSpace> parkingSpaces = parkingSpaceService.getAllParkingSpaces();
			return ResponseEntity.ok(parkingSpaces);
		} catch (Exception e) {
			// return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
			e.getMessage();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/add")
	public ResponseEntity<Object> addParkingSpaces(@RequestBody ParkingSpace parkingSpace) {
		try {
			ParkingSpace addParkingSpaces = parkingSpaceService.addParkingSpace(parkingSpace);
			return ResponseEntity.ok(addParkingSpaces);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error adding new Parking space: " + e.getMessage());
		}
	}

	@GetMapping(value = "/details/space/{id}")
	public ResponseEntity<Object> getParkingSpaceById(@PathVariable("id") Long id) {
		try {
			return ResponseEntity.ok(parkingSpaceService.getParkingSpaceById(id));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Parking space not found with ID: " + id);
		}
	}

	@DeleteMapping("/remove/{id}")
	public ResponseEntity<Object> deleteParkingSpaceById(@PathVariable("id") Long id) {
		try {
			parkingSpaceService.deleteParkingSpace(id);
			return ResponseEntity.ok("Parking space deleted successfully");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Parking space not found with ID: " + id);
		}
	}
}
