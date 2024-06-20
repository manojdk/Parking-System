package com.parkingSystem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.repository.ParkingSpaceRepository;
import com.parkingSystem.repository.ReservationRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ReservationService {

	private ReservationRepository reservationRepository;

	private ParkingSpaceRepository parkingSpaceRepository;

	private ParkingSpaceService parkingSpaceService;

	private UserService userService;

	public List<Reservation> getAllReservations() {
		try {
			return reservationRepository.findAll();
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching all reservations: " + e.getMessage());
		}
	}

	@Transactional
	public Reservation createReservation(Long userId, Long parkingSpaceId, LocalDateTime reservationTime,
			Integer durationMinutes) {
		try {
			User user = userService.getUserById(userId)
					.orElseThrow(() -> new IllegalArgumentException("User not found"));
			ParkingSpace parkingSpace = parkingSpaceService.getParkingSpaceById(parkingSpaceId)
					.orElseThrow(() -> new IllegalArgumentException("Parking Space not found"));

			if (!"available".equals(parkingSpace.getAvailabilityStatus())) {
				throw new IllegalArgumentException("Parking space is not available");
			}

			Reservation reservation = new Reservation();
			reservation.setUser(user);
			reservation.setParkingSpace(parkingSpace);
			reservation.setReservationTime(reservationTime);
			reservation.setDuration(durationMinutes);

			parkingSpace.setAvailabilityStatus("occupied");
			parkingSpaceRepository.save(parkingSpace);

			return reservationRepository.save(reservation);

		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while making reservation: " + e.getMessage());
		}
	}

	public Reservation updateReservation(Long reservationId, LocalDateTime reservationTime, Long parkingSpaceId,
			Integer duration) {
		try {
			Reservation reservation = getReservationById(reservationId);
			ParkingSpace parkingSpace = parkingSpaceRepository.findById(parkingSpaceId).orElseThrow(
					() -> new IllegalArgumentException("Parking space not found with id: " + parkingSpaceId));

			reservation.setParkingSpace(parkingSpace);
			reservation.setReservationTime(reservationTime);
			reservation.setDuration(duration);

			parkingSpace.setAvailabilityStatus("occupied");
			parkingSpaceRepository.save(parkingSpace);

			return reservationRepository.save(reservation);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while updating reservation: " + e.getMessage());
		}
	}

	@Transactional
	public void deleteReservation(Long reservationId) {
		try {
			Reservation reservation = getReservationById(reservationId);
			ParkingSpace parkingSpace = reservation.getParkingSpace();
			parkingSpace.setAvailabilityStatus("available");
			parkingSpaceRepository.save(parkingSpace);

			reservationRepository.deleteById(reservationId);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while deleting reservation: " + e.getMessage());
		}
	}

	public Reservation getReservationById(Long reservationId) {
		try {
			return reservationRepository.findByReservationId(reservationId);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching reservation by ID: " + e.getMessage());
		}
	}

	public List<Reservation> getReservationsByUserId(User userId) {
		try {
			return reservationRepository.findAllByUser(userId);
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Database error occurred while fetching reservations by user ID: " + e.getMessage());
		}
	}

}
