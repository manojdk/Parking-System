package com.parkingSystem.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.parkingSystem.enums.ReservationStatus;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;

import jakarta.persistence.LockModeType;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	List<Reservation> findAllByUser(User user);

	List<Reservation> findAllByParkingSpace(ParkingSpace parkingSpace);

	Reservation findByReservationId(Long reservationId);

	List<Reservation> findAllByUserUserId(Long userId);

	/**
	 * The space a reservation points at, read without loading the reservation, so the caller can lock the
	 * space row first and the reservation second (the one lock order used everywhere, to avoid deadlocks).
	 */
	@Query("SELECT r.parkingSpace.parkingSpaceId FROM Reservation r WHERE r.reservationId = :id")
	Optional<Long> findSpaceIdByReservationId(@Param("id") Long reservationId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Reservation r WHERE r.reservationId = :id")
	Optional<Reservation> findByIdForUpdate(@Param("id") Long reservationId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Reservation r WHERE r.parkingSpace.parkingSpaceId = :spaceId"
			+ " AND r.status = com.parkingSystem.enums.ReservationStatus.ACTIVE")
	Optional<Reservation> findActiveBySpaceIdForUpdate(@Param("spaceId") Long parkingSpaceId);

	/** Id of the user's ACTIVE reservation, read without loading (and caching) the entity before locking. */
	@Query("SELECT r.reservationId FROM Reservation r WHERE r.user.userId = :userId"
			+ " AND r.status = com.parkingSystem.enums.ReservationStatus.ACTIVE")
	Optional<Long> findActiveIdByUserId(@Param("userId") Long userId);

	boolean existsByUserUserIdAndStatus(Long userId, ReservationStatus status);

	boolean existsByParkingSpaceParkingSpaceId(Long parkingSpaceId);

	/** Ids of ACTIVE reservations whose hold ran out at or before {@code now}. */
	@Query("SELECT r.reservationId FROM Reservation r WHERE r.status = com.parkingSystem.enums.ReservationStatus.ACTIVE"
			+ " AND r.expiresAt <= :now ORDER BY r.expiresAt")
	List<Long> findExpiredActiveIds(@Param("now") LocalDateTime now);
}
