package com.parkingSystem.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.uuidTest.ParkingSpaceRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ParkingSpaceService {

	private ParkingSpaceRepository parkingSpaceRepository;

	/**
	 * Get all Parking Spaces details
	 * 
	 * @return Parking space data
	 */
	public List<ParkingSpace> getAllParkingSpaces() {
		try {
			return parkingSpaceRepository.findAll();
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching all parking spaces: " + e.getMessage());
		}
	}

	/**
	 * Get Parking space detail by parkingSpaceId (helper class)
	 * 
	 * @param parkingSpaceId Parking space ID
	 * @return Parking space data
	 */
	public Optional<ParkingSpace> getParkingSpaceById(long parkingSpaceId) {
		try {
			return parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching parking spaces by ID: " + e.getMessage());
		}
	}

	/**
	 * Get available Parking space detail
	 * 
	 * @param availabilityStatus Status available or occupied
	 * @return Parking space data
	 */
	public List<ParkingSpace> getAvailableParkingSpaces(String availabilityStatus) {
		try {
			return parkingSpaceRepository.findByAvailabilityStatus(availabilityStatus);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching available parking spaces: " + e.getMessage());
		}
	}

	/**
	 * Get Parking Space detail by parkingSpaceId
	 * 
	 * @param parkingSpaceId Parking space ID
	 * @return Parking space data
	 */
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
			parkingSpaceDetails.put("availabilityStatus", parkingSpace.getAvailabilityStatus());

			return parkingSpaceDetails;
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching parking space details: " + e.getMessage());
		}
	}

	/**
	 * 
	 * @param parkingSpace
	 * @return
	 */
	@Transactional
	public ParkingSpace addParkingSpace(ParkingSpace parkingSpace) {
		try {
			parkingSpace.setLocation(parkingSpace.getLocation());
			parkingSpace.setType(parkingSpace.getType());
			parkingSpace.setRate(parkingSpace.getRate());
			parkingSpace.setAvailabilityStatus("available");

			return parkingSpaceRepository.save(parkingSpace);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while adding parking space: " + e.getMessage());
		}
	}

	/**
	 * 
	 * @param parkingSpaceRequests
	 * @return
	 */
	@Transactional
	public List<ParkingSpace> addMultipleParkingSpaces(List<ParkingSpace> parkingSpaceRequests) {
		try {
			List<ParkingSpace> parkingSpaces = parkingSpaceRequests.stream().map(request -> {
				ParkingSpace parkingSpace = new ParkingSpace();
				parkingSpace.setLocation(request.getLocation());
				parkingSpace.setType(request.getType());
				parkingSpace.setRate(request.getRate());
				parkingSpace.setAvailabilityStatus("available");
				return parkingSpace;
			}).collect(Collectors.toList());
			return parkingSpaceRepository.saveAll(parkingSpaces);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while adding multiple parking spaces: " + e.getMessage());
		}
	}

	/**
	 * 
	 * @param parkingSpaceId
	 * @param location
	 * @param type
	 * @param rate
	 * @param availabilityStatus
	 * @return
	 */
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
				parkingSpace.setAvailabilityStatus(availabilityStatus ? "available" : "occupied");

			return parkingSpaceRepository.save(parkingSpace);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while updating parking space: " + e.getMessage());
		}
	}

	/**
	 * Delete Parking space by Parking space Id
	 * 
	 * @param parkingSpaceId Parking space ID
	 */
	@Transactional
	public void deleteParkingSpace(Long parkingSpaceId) {
		try {
			parkingSpaceRepository.deleteByParkingSpaceId(parkingSpaceId);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while deleting the parking space: " + e.getMessage());
		}
	}
}
