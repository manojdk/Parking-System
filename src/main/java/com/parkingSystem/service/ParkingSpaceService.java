package com.parkingSystem.service;

import java.util.List;
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

	@Transactional
	public ParkingSpace addParkingSpace(ParkingSpace parkingSpace) {
		try {
			return parkingSpaceRepository.save(parkingSpace);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while adding new parking space: " + e.getMessage());
		}
	}

	@Transactional
	public void updateParkingSpace(ParkingSpace parkingSpace) {
		try {
			parkingSpaceRepository.save(parkingSpace);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while updating the parking space: " + e.getMessage());
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
