package com.parkingSystem.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.repository.ReservationRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ReservationService {

	private ReservationRepository reservationRepository;

	private ParkingSpaceService parkingSpaceService;

	private UserService userService;

	@Transactional
	public Reservation makeReservation(Long userId, Long parkingSpaceId, LocalDateTime reservationTime,
			Integer durationMinutes) {
		try {
			User user = userService.getUserById(userId)
					.orElseThrow(() -> new IllegalArgumentException("User not found"));
			ParkingSpace parkingSpace = parkingSpaceService.getParkingSpaceById(parkingSpaceId)
					.orElseThrow(() -> new IllegalArgumentException("PArking Space not found"));

			if (!parkingSpace.isAvailabilityStatus()) {
				throw new IllegalStateException("Parking space is already occupied.");
			}

			Reservation reservation = new Reservation();

			reservation.setUser(user);
			reservation.setParkingSpace(parkingSpace);
			reservation.setReservationTime(reservationTime);
			reservation.setDuration(durationMinutes);

			parkingSpace.setAvailabilityStatus(false);
			parkingSpaceService.updateParkingSpace(parkingSpace);
			return reservationRepository.save(reservation);

		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while making reservation: " + e.getMessage());
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw e;
		}
	}

	@Transactional
	public void cancelReservation(Long reservationId) {
		try {
			Reservation reservation = reservationRepository.findByReservationId(reservationId);

			// Mark parking space as available
			ParkingSpace parkingSpace = reservation.getParkingSpace();
			parkingSpace.setAvailabilityStatus(true);
			parkingSpaceService.updateParkingSpace(parkingSpace);

			// Delete the reservation
			reservationRepository.delete(reservation);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while canceling reservation: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
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
