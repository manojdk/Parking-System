package com.parkingSystem.service;

import com.parkingSystem.model.Reservation;
import java.util.List;

/**
 * Service interface for managing reservations.
 * <p>
 * A reservation holds an AVAILABLE space (it becomes RESERVED) for a limited time. It ends when the owner
 * drives in (FULFILLED, handled by vehicle entry), when it is cancelled (CANCELLED) or when the hold runs
 * out (EXPIRED).
 */
public interface IReservationService {

    /**
     * Get all Reservation details
     *
     * @return Reservation data
     */
    List<Reservation> getAllReservations();

    /**
     * Reserve an AVAILABLE space for the user. A user can hold only one active reservation at a time.
     *
     * @param userId User ID
     * @param parkingSpaceId Parking space ID
     * @param duration Expected stay in hours (informational)
     * @return the new ACTIVE reservation
     */
    Reservation createReservation(Long userId, Long parkingSpaceId, Integer duration);

    /**
     * Move an ACTIVE reservation to another AVAILABLE space and/or change the expected duration. The hold
     * time is not extended.
     *
     * @param reservationId Reservation ID
     * @param parkingSpaceId Parking space ID (the current one to keep it)
     * @param duration Duration, or null to keep the current one
     * @return Updated reservation
     */
    Reservation updateReservation(Long reservationId, Long parkingSpaceId, Integer duration);

    /**
     * Cancel an ACTIVE reservation and release its space. The record is kept.
     *
     * @param reservationId Reservation ID
     * @return the cancelled reservation
     */
    Reservation cancelReservation(Long reservationId);

    /**
     * Expire one reservation if its hold has run out and it is still ACTIVE; otherwise do nothing.
     *
     * @param reservationId Reservation ID
     * @return true if it was expired by this call
     */
    boolean expireReservation(Long reservationId);

    /**
     * @return ids of ACTIVE reservations whose hold has run out
     */
    List<Long> findOverdueReservationIds();

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
     * @param userId User ID
     * @return List of reservations
     */
    List<Reservation> getReservationsByUserId(Long userId);
}
