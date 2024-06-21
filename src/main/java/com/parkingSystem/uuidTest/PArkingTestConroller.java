package com.parkingSystem.uuidTest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parkingSystem.repository.ParkingTestRepo;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/test")
@AllArgsConstructor
public class PArkingTestConroller {

	private ParkingTestRepo parkingSpaceRepository;

	@PostMapping("/uuid")
	public ResponseEntity<ParkingSpaceTest> createParkingSpace(@RequestBody ParkingSpaceTest parkingSpace) {
		try {
			ParkingSpaceTest createdParkingSpace = parkingSpaceRepository.save(parkingSpace);
			return new ResponseEntity<>(createdParkingSpace, HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
