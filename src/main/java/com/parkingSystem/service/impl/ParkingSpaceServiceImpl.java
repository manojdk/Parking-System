package com.parkingSystem.service.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.repository.ParkingSpaceRepository;
import com.parkingSystem.repository.ParkingTicketRepository;
import com.parkingSystem.repository.ReservationRepository;
import com.parkingSystem.service.IParkingSpaceService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ParkingSpaceServiceImpl implements IParkingSpaceService {

	private ParkingSpaceRepository parkingSpaceRepository;
	private ParkingTicketRepository parkingTicketRepository;
	private ReservationRepository reservationRepository;

	/**
	 * Get all Parking Spaces details
	 * 
	 * @return Parking space data
	 */
	public Page<ParkingSpace> getAllParkingSpaces(Pageable pageable) {
		return parkingSpaceRepository.findAll(pageable);
	}

	/**
	 * Get Parking space detail by parkingSpaceId (helper class)
	 * 
	 * @param parkingSpaceId Parking space ID
	 * @return Parking space data
	 */
	public Optional<ParkingSpace> getParkingSpaceById(long parkingSpaceId) {
		return parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId);
	}

	/**
	 * In-service parking spaces with the given status
	 *
	 * @param availabilityStatus status to filter on
	 * @return Parking space data
	 */
	public Page<ParkingSpace> getAvailableParkingSpaces(SpaceStatus availabilityStatus, Pageable pageable) {
		return parkingSpaceRepository.findByAvailabilityStatusAndIsActiveTrue(availabilityStatus, pageable);
	}

	/**
	 * Get Parking Space detail by parkingSpaceId
	 * 
	 * @param parkingSpaceId Parking space ID
	 * @return Parking space data
	 */
	public ParkingSpace getParkingSpaceDetails(Long parkingSpaceId) {
		return getParkingSpaceById(parkingSpaceId)
				.orElseThrow(() -> new ResourceNotFoundException("Parking space not found with id: " + parkingSpaceId));
	}

	/**
	 * Adding Parking space
	 * 
	 * @param parkingSpace {@link ParkingSpace}
	 * @return save Parking space data
	 */
	@Transactional
	public ParkingSpace addParkingSpace(ParkingSpace parkingSpace) {
		if (parkingSpaceRepository.existsBySpaceNumber(parkingSpace.getSpaceNumber())) {
			throw new ConflictException("Space number already exists: " + parkingSpace.getSpaceNumber());
		}
		return parkingSpaceRepository.save(newAvailableSpace(parkingSpace));
	}

	/**
	 * Add list of Parking space
	 * 
	 * @param parkingSpaceRequests list of {@link ParkingSpace}
	 * @return List of saved Parking space data
	 */
	@Transactional
	public List<ParkingSpace> addMultipleParkingSpaces(List<ParkingSpace> parkingSpaceRequests) {
		Set<String> spaceNumbers = new HashSet<>();
		for (ParkingSpace request : parkingSpaceRequests) {
			if (!spaceNumbers.add(request.getSpaceNumber())
					|| parkingSpaceRepository.existsBySpaceNumber(request.getSpaceNumber())) {
				throw new ConflictException("Duplicate space number: " + request.getSpaceNumber());
			}
		}
		List<ParkingSpace> parkingSpaces = parkingSpaceRequests.stream()
				.map(ParkingSpaceServiceImpl::newAvailableSpace)
				.collect(Collectors.toList());
		return parkingSpaceRepository.saveAll(parkingSpaces);
	}

	/**
	 * Copies the client supplied fields into a new, active and available parking space.
	 */
	private static ParkingSpace newAvailableSpace(ParkingSpace request) {
		ParkingSpace parkingSpace = new ParkingSpace();
		parkingSpace.setSpaceNumber(request.getSpaceNumber());
		parkingSpace.setLocation(request.getLocation());
		parkingSpace.setFloorNumber(request.getFloorNumber());
		parkingSpace.setSlotType(request.getSlotType());
		parkingSpace.setRatePerHour(request.getRatePerHour());
		parkingSpace.setNotes(request.getNotes());
		parkingSpace.setIsActive(true);
		parkingSpace.setAvailabilityStatus(SpaceStatus.AVAILABLE);
		return parkingSpace;
	}

	/**
	 * Update Parking Space Data. The row is locked so a status change cannot race a vehicle entry or a
	 * reservation for the same space.
	 *
	 * @param parkingSpaceId Parking space ID
	 * @param location       Location of parking space
	 * @param type           Type of space
	 * @param rate           Rate of parking space
	 * @param status         AVAILABLE or OUT_OF_SERVICE; null keeps the current status
	 * @return Updated Parking space data
	 */
	@Transactional
	public ParkingSpace updateParkingSpace(Long parkingSpaceId, String location, SlotType type, BigDecimal rate,
			SpaceStatus status) {
		ParkingSpace parkingSpace = lockSpace(parkingSpaceId);
		if (!Boolean.TRUE.equals(parkingSpace.getIsActive())) {
			throw new ConflictException("Parking space has been removed and cannot be changed");
		}
		if (location != null)
			parkingSpace.setLocation(location);
		if (type != null)
			parkingSpace.setSlotType(type);
		if (rate != null)
			parkingSpace.setRatePerHour(rate);
		if (status != null)
			changeStatus(parkingSpace, status);
		return parkingSpaceRepository.save(parkingSpace);
	}

	/**
	 * Admins only switch between AVAILABLE and OUT_OF_SERVICE. RESERVED and OCCUPIED belong to reservations
	 * and tickets; changing them by hand would leave a ticket or reservation pointing at a "free" space.
	 */
	private static void changeStatus(ParkingSpace parkingSpace, SpaceStatus target) {
		if (target != SpaceStatus.AVAILABLE && target != SpaceStatus.OUT_OF_SERVICE) {
			throw new BadRequestException("Status can only be set to AVAILABLE or OUT_OF_SERVICE; "
					+ "RESERVED and OCCUPIED are managed by reservations and vehicle entry");
		}
		SpaceStatus current = parkingSpace.getAvailabilityStatus();
		if (current == SpaceStatus.RESERVED || current == SpaceStatus.OCCUPIED) {
			throw new ConflictException("Parking space is currently " + current
					+ "; wait for the reservation or ticket to end before changing its status");
		}
		parkingSpace.setAvailabilityStatus(target);
	}

	/**
	 * Remove a parking space. Spaces with history are deactivated rather than deleted, so old tickets,
	 * bills and reservations keep a valid space; a space in use cannot be removed.
	 *
	 * @param parkingSpaceId Parking space ID
	 */
	@Transactional
	public void deleteParkingSpace(Long parkingSpaceId) {
		ParkingSpace parkingSpace = lockSpace(parkingSpaceId);
		SpaceStatus current = parkingSpace.getAvailabilityStatus();
		if (current == SpaceStatus.RESERVED || current == SpaceStatus.OCCUPIED) {
			throw new ConflictException("Parking space is currently " + current + " and cannot be removed");
		}
		if (parkingTicketRepository.existsByParkingSpaceParkingSpaceId(parkingSpaceId)
				|| reservationRepository.existsByParkingSpaceParkingSpaceId(parkingSpaceId)) {
			parkingSpace.setIsActive(false);
			parkingSpace.setAvailabilityStatus(SpaceStatus.OUT_OF_SERVICE);
			parkingSpaceRepository.save(parkingSpace);
		} else {
			parkingSpaceRepository.delete(parkingSpace);
		}
	}

	private ParkingSpace lockSpace(Long parkingSpaceId) {
		return parkingSpaceRepository.findByIdForUpdate(parkingSpaceId)
				.orElseThrow(() -> new ResourceNotFoundException("Parking space not found with id: " + parkingSpaceId));
	}
}
