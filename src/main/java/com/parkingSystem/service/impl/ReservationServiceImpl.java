package com.parkingSystem.service.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkingSystem.enums.ReservationStatus;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.repository.ParkingSpaceRepository;
import com.parkingSystem.repository.ReservationRepository;
import com.parkingSystem.repository.VehicleRepository;
import com.parkingSystem.service.IReservationService;
import com.parkingSystem.service.IUserService;

import lombok.extern.slf4j.Slf4j;

/**
 * Reservation life cycle.
 * <p>
 * Concurrency: every method that changes a reservation first locks the parking-space row(s) involved, in
 * ascending id order, and only then locks and reads the reservation. Vehicle entry and the expiry job use
 * the same order (space, then reservation), so two requests touching the same space run one after the other
 * and cannot deadlock. Ids are looked up with id-only queries before locking, so no entity is read (and
 * cached) before its lock is held. The unique "active" columns (V4 migration) back this up in the database.
 */
@Service
@Slf4j
public class ReservationServiceImpl implements IReservationService {

	private final ReservationRepository reservationRepository;
	private final ParkingSpaceRepository parkingSpaceRepository;
	private final IUserService userService;
	private final VehicleRepository vehicleRepository;
	private final Clock clock;
	private final Duration holdTime;

	public ReservationServiceImpl(ReservationRepository reservationRepository,
			ParkingSpaceRepository parkingSpaceRepository, IUserService userService,
			VehicleRepository vehicleRepository, Clock clock,
			@Value("${app.reservation.hold-minutes:30}") long holdMinutes) {
		if (holdMinutes <= 0) {
			throw new IllegalStateException("app.reservation.hold-minutes must be positive, was " + holdMinutes);
		}
		this.reservationRepository = reservationRepository;
		this.parkingSpaceRepository = parkingSpaceRepository;
		this.userService = userService;
		this.vehicleRepository = vehicleRepository;
		this.clock = clock;
		this.holdTime = Duration.ofMinutes(holdMinutes);
	}

	/**
	 * Get all Reservation details
	 *
	 * @return Reservation data
	 */
	public Page<Reservation> getAllReservations(Pageable pageable) {
		return reservationRepository.findAll(pageable);
	}

	/**
	 * Reserve a space. The space must be in service and AVAILABLE, and the user must not already hold an
	 * active reservation. If the user's previous reservation has run out but the expiry job has not
	 * processed it yet, it is expired here instead of blocking the new one.
	 */
	@Transactional
	public Reservation createReservation(Long userId, Long parkingSpaceId, Integer duration) {
		Long previousId = reservationRepository.findActiveIdByUserId(userId).orElse(null);
		Long previousSpaceId = previousId == null ? null
				: reservationRepository.findSpaceIdByReservationId(previousId).orElse(null);

		Map<Long, ParkingSpace> spaces = lockSpaces(parkingSpaceId, previousSpaceId);
		ParkingSpace parkingSpace = spaces.get(parkingSpaceId);
		User user = userService.getUserById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
		LocalDateTime now = now();

		if (previousId != null) {
			Reservation previous = lockReservation(previousId);
			if (previous.isHoldOver(now)) {
				expire(previous, spaces.get(previousSpaceId));
			} else if (previous.isActive()) {
				throw new ConflictException("You already have an active reservation (ID " + previousId
						+ "). Cancel it before reserving another space");
			}
		}
		requireInService(parkingSpace);
		requireFittingVehicle(userId, parkingSpace);
		if (parkingSpace.getAvailabilityStatus() != SpaceStatus.AVAILABLE) {
			throw new ConflictException("Parking space is not available. Current status: "
					+ parkingSpace.getAvailabilityStatus());
		}

		Reservation reservation = new Reservation();
		reservation.setUser(user);
		reservation.setParkingSpace(parkingSpace);
		reservation.setReservationTime(now);
		reservation.setDuration(duration);
		reservation.setStatus(ReservationStatus.ACTIVE);
		reservation.setExpiresAt(now.plus(holdTime));

		parkingSpace.setAvailabilityStatus(SpaceStatus.RESERVED);
		parkingSpaceRepository.save(parkingSpace);
		Reservation saved = reservationRepository.save(reservation);
		log.info("Reservation {} holds space {} for user {} until {}", saved.getReservationId(),
				parkingSpaceId, userId, saved.getExpiresAt());
		return saved;
	}

	/**
	 * Update an ACTIVE reservation: move it to another AVAILABLE space and/or change the expected duration.
	 * The hold is not extended, otherwise a driver could keep a space forever by moving between spaces.
	 */
	@Transactional
	public Reservation updateReservation(Long reservationId, Long parkingSpaceId, Integer duration) {
		Long currentSpaceId = spaceIdOf(reservationId);
		Map<Long, ParkingSpace> spaces = lockSpaces(currentSpaceId, parkingSpaceId);
		Reservation reservation = lockReservation(reservationId);
		requireOpen(reservation);

		if (!currentSpaceId.equals(parkingSpaceId)) {
			ParkingSpace currentSpace = spaces.get(currentSpaceId);
			ParkingSpace newSpace = spaces.get(parkingSpaceId);
			requireInService(newSpace);
			requireFittingVehicle(reservation.getUser().getUserId(), newSpace);
			if (newSpace.getAvailabilityStatus() != SpaceStatus.AVAILABLE) {
				throw new ConflictException("Parking space is not available. Current status: "
						+ newSpace.getAvailabilityStatus());
			}
			release(currentSpace);
			newSpace.setAvailabilityStatus(SpaceStatus.RESERVED);
			parkingSpaceRepository.save(currentSpace);
			parkingSpaceRepository.save(newSpace);
			reservation.setParkingSpace(newSpace);
		}
		if (duration != null) {
			reservation.setDuration(duration);
		}
		return reservationRepository.save(reservation);
	}

	/**
	 * Cancel an ACTIVE reservation and release its space. The row is kept (status CANCELLED) so history and
	 * feedback stay intact.
	 */
	@Transactional
	public Reservation cancelReservation(Long reservationId) {
		Long spaceId = spaceIdOf(reservationId);
		ParkingSpace parkingSpace = lockSpaces(spaceId).get(spaceId);
		Reservation reservation = lockReservation(reservationId);
		if (!reservation.isActive()) {
			throw new ConflictException("Only active reservations can be cancelled. Current status: "
					+ reservation.getStatus());
		}
		reservation.cancel(now());
		release(parkingSpace);
		parkingSpaceRepository.save(parkingSpace);
		log.info("Reservation {} cancelled, space {} released", reservationId, spaceId);
		return reservationRepository.save(reservation);
	}

	@Transactional
	public boolean expireReservation(Long reservationId) {
		Long spaceId = reservationRepository.findSpaceIdByReservationId(reservationId).orElse(null);
		if (spaceId == null) {
			return false;
		}
		ParkingSpace parkingSpace = lockSpaces(spaceId).get(spaceId);
		Reservation reservation = lockReservation(reservationId);
		// Re-checked under the lock: the owner may have driven in, or another instance may have expired it
		if (!reservation.isHoldOver(now())) {
			return false;
		}
		expire(reservation, parkingSpace);
		return true;
	}

	public List<Long> findOverdueReservationIds() {
		return reservationRepository.findExpiredActiveIds(now());
	}

	/**
	 * Get reservation detail by reservationId
	 *
	 * @param reservationId
	 * @return Reservation data
	 */
	public Reservation getReservationById(Long reservationId) {
		Reservation reservation = reservationRepository.findByReservationId(reservationId);
		if (reservation == null) {
			throw new ResourceNotFoundException("Reservation not found with ID: " + reservationId);
		}
		return reservation;
	}

	/**
	 * Get reservation detail by UserId
	 *
	 * @param userId
	 * @return Reservation data
	 */
	public Page<Reservation> getReservationsByUserId(Long userId, Pageable pageable) {
		return reservationRepository.findAllByUserUserId(userId, pageable);
	}

	// ------------------------------------------------------------------ helpers

	/** Truncated to the database's precision, so values returned now equal the values read back later. */
	private LocalDateTime now() {
		return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
	}

	private Long spaceIdOf(Long reservationId) {
		return reservationRepository.findSpaceIdByReservationId(reservationId)
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));
	}

	/**
	 * Locks the given spaces (nulls and duplicates ignored) in ascending id order, the order used by every
	 * writer, so concurrent transactions cannot deadlock on them.
	 */
	private Map<Long, ParkingSpace> lockSpaces(Long... parkingSpaceIds) {
		Map<Long, ParkingSpace> locked = new TreeMap<>();
		Stream.of(parkingSpaceIds).filter(Objects::nonNull).distinct().sorted().forEach(id -> locked.put(id,
				parkingSpaceRepository.findByIdForUpdate(id).orElseThrow(
						() -> new ResourceNotFoundException("Parking space not found with id: " + id))));
		return locked;
	}

	private Reservation lockReservation(Long reservationId) {
		return reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));
	}

	private void requireOpen(Reservation reservation) {
		if (!reservation.isActive()) {
			throw new ConflictException("Only active reservations can be changed. Current status: "
					+ reservation.getStatus());
		}
		if (reservation.isHoldOver(now())) {
			throw new ConflictException("Reservation has expired at " + reservation.getExpiresAt());
		}
	}

	/**
	 * A reservation is only useful if the driver can actually park there: they need an active vehicle that the
	 * space's slot type accepts (for example an EV for a charging bay).
	 */
	private void requireFittingVehicle(Long userId, ParkingSpace parkingSpace) {
		boolean fits = vehicleRepository.findByUserUserIdAndIsActiveTrue(userId).stream()
				.anyMatch(vehicle -> parkingSpace.getSlotType().accepts(vehicle.getVehicleType()));
		if (!fits) {
			throw new BadRequestException("You have no registered vehicle that can park in a "
					+ parkingSpace.getSlotType() + " space");
		}
	}

	private static void requireInService(ParkingSpace parkingSpace) {
		if (!Boolean.TRUE.equals(parkingSpace.getIsActive())) {
			throw new BadRequestException("Parking space is not in service");
		}
	}

	private void expire(Reservation reservation, ParkingSpace parkingSpace) {
		reservation.expire();
		release(parkingSpace);
		parkingSpaceRepository.save(parkingSpace);
		// Flushed now: a new reservation for the same user or space may be inserted next in this transaction,
		// and the insert would otherwise reach the database before this update clears the unique guard columns.
		reservationRepository.saveAndFlush(reservation);
		log.info("Reservation {} expired (no-show), space {} released", reservation.getReservationId(),
				parkingSpace.getParkingSpaceId());
	}

	/** A space held by a reservation goes back to AVAILABLE; any other status is left alone. */
	static void release(ParkingSpace parkingSpace) {
		if (parkingSpace.getAvailabilityStatus() == SpaceStatus.RESERVED) {
			parkingSpace.setAvailabilityStatus(SpaceStatus.AVAILABLE);
		}
	}
}
