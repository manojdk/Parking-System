package com.parkingSystem.service;

import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import java.util.List;

/**
 * Service interface for managing reservations
 */
public interface IReservationService {

    /**
     * Get all Reservation details
     *
     * @return Reservation data
     */
    List<Reservation> getAllReservations();

    /**
     * Create reservation
     *
     * @param userId User ID
     * @param parkingSpaceId Parking space ID
     * @param duration Duration
     * @return Reservation
     */
    Reservation createReservation(Long userId, Long parkingSpaceId, Integer duration);

    /**
     * Update reservation details
     *
     * @param reservationId Reservation ID
     * @param parkingSpaceId Parking space ID
     * @param duration Duration
     * @return Updated reservation
     */
    Reservation updateReservation(Long reservationId, Long parkingSpaceId, Integer duration);

    /**
     * Delete reservation
     *
     * @param reservationId Reservation ID
     */
    void deleteReservation(Long reservationId);

    /**
     * Get reservation by ID
     *
     * @param reservationId Reservation ID
     * @return Reservation
     */
    Reservation getReservationById(Long reservationId);

    /**
     * Get reservations by user ID
     *
     * @param userId User
     * @return List of reservations
     */
    List<Reservation> getReservationsByUserId(User userId);
}
