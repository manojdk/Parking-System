package com.parkingSystem.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.repository.ParkingSpaceRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ParkingSpaceService {

	private ParkingSpaceRepository parkingSpaceRepository;

	public List<ParkingSpace> getAllParkingSpaces() {
		try {
			return parkingSpaceRepository.findAll();
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching all parking spaces: " + e.getMessage());
		}
	}

	public List<ParkingSpace> getAvailableParkingSpaces() {
		try {
			return parkingSpaceRepository.findAvailableParkingSpaces();
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching available parking spaces: " + e.getMessage());
		}
	}

	public Optional<ParkingSpace> getParkingSpaceById(long parkingSpaceId) {
		try {
			return parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching parking spaces by ID: " + e.getMessage());
		}
	}

	public Map<String, Object> getParkingSpaceDetails(Long parkingSpaceId) {
		try {
			Optional<ParkingSpace> parkingSpaceOptional = getParkingSpaceById(parkingSpaceId);
			if (parkingSpaceOptional.isEmpty()) {
				throw new IllegalArgumentException("Parking space not found with id: " + parkingSpaceId);
			}

			ParkingSpace parkingSpace = parkingSpaceOptional.get();
			Map<String, Object> parkingSpaceDetails = new HashMap<>();
			parkingSpaceDetails.put("parkingSpaceId", parkingSpace.getParkingSpaceId());
			parkingSpaceDetails.put("location", parkingSpace.getLocation());
			parkingSpaceDetails.put("type", parkingSpace.getType());
			parkingSpaceDetails.put("rate", parkingSpace.getRate());
			parkingSpaceDetails.put("availabilityStatus", parkingSpace.isAvailabilityStatus());

			return parkingSpaceDetails;
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching parking space details: " + e.getMessage());
		}
	}

	@Transactional
	public ParkingSpace addParkingSpace(String location, String type, Double rate) {
		try {
			ParkingSpace parkingSpace = new ParkingSpace();
			parkingSpace.setLocation(location);
			parkingSpace.setType(type);
			parkingSpace.setRate(rate);
			parkingSpace.setAvailabilityStatus(true); // assuming new parking space is available

			return parkingSpaceRepository.save(parkingSpace);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while adding parking space: " + e.getMessage());
		}
	}

	@Transactional
	public ParkingSpace updateParkingSpace(Long parkingSpaceId, String location, String type, Double rate,
			Boolean availabilityStatus) {
		try {
			Optional<ParkingSpace> parkingSpaceOptional = parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId);
			if (parkingSpaceOptional.isEmpty()) {
				throw new IllegalArgumentException("Parking space not found with id: " + parkingSpaceId);
			}

			ParkingSpace parkingSpace = parkingSpaceOptional.get();
			if (location != null)
				parkingSpace.setLocation(location);
			if (type != null)
				parkingSpace.setType(type);
			if (rate != null)
				parkingSpace.setRate(rate);
			if (availabilityStatus != null)
				parkingSpace.setAvailabilityStatus(availabilityStatus);

			return parkingSpaceRepository.save(parkingSpace);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while updating parking space: " + e.getMessage());
		}
	}

	@Transactional
	public void deleteParkingSpace(Long parkingSpaceId) {
		try {
			parkingSpaceRepository.deleteByParkingSpaceId(parkingSpaceId);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while deleting the parking space: " + e.getMessage());
		}
	}
}
