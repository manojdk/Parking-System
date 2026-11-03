package com.parkingSystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

	List<ParkingSpace> findByAvailabilityStatusAndIsActiveTrue(SpaceStatus availabilityStatus);

	Optional<ParkingSpace> findByParkingSpaceId(Long parkingSpaceId);

	/**
	 * Loads the space and locks its row (SELECT ... FOR UPDATE) until the transaction ends. Every change of
	 * a space's status goes through this, so two requests for the same space are handled one after the
	 * other instead of both seeing it as free. Call it before anything else in the transaction loads the
	 * space, so the state it returns is read under the lock.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM ParkingSpace p WHERE p.parkingSpaceId = :id")
	Optional<ParkingSpace> findByIdForUpdate(@Param("id") Long id);

	void deleteByParkingSpaceId(Long parkingSpaceId);

	boolean existsBySpaceNumber(String spaceNumber);

	@Query("SELECT p FROM ParkingSpace p WHERE p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true")
	List<ParkingSpace> findAvailableParkingSpaces();

	List<ParkingSpace> findAll();

	// Slot type queries
	List<ParkingSpace> findBySlotType(SlotType slotType);

	@Query("SELECT p FROM ParkingSpace p WHERE p.slotType = :slotType AND p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true")
	List<ParkingSpace> findAvailableBySlotType(@Param("slotType") SlotType slotType);

	// Location-based queries
	@Query("SELECT p FROM ParkingSpace p WHERE p.location = :location AND p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true")
	List<ParkingSpace> findAvailableByLocation(@Param("location") String location);

	// Smart slot allocation - find nearest available slot
	@Query("SELECT p FROM ParkingSpace p WHERE p.location = :location AND p.slotType = :slotType AND p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true ORDER BY p.floorNumber ASC LIMIT 1")
	Optional<ParkingSpace> findNearestAvailableSlot(@Param("location") String location, @Param("slotType") SlotType slotType);

	// Count available slots by type
	@Query("SELECT COUNT(p) FROM ParkingSpace p WHERE p.slotType = :slotType AND p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true")
	Long countAvailableBySlotType(@Param("slotType") SlotType slotType);

	// Get all available slots count
	@Query("SELECT COUNT(p) FROM ParkingSpace p WHERE p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true")
	Long countAllAvailable();

	List<ParkingSpace> findByIsActiveTrueOrderByFloorNumber();

	@Query("SELECT p FROM ParkingSpace p WHERE p.location = :location AND p.slotType = :slotType AND p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true ORDER BY p.floorNumber ASC")
    List<ParkingSpace> findNearestAvailableSlots(@Param("location") String location, @Param("slotType") SlotType slotType);

	// ---- paginated (step 6)

	Page<ParkingSpace> findByAvailabilityStatusAndIsActiveTrue(SpaceStatus availabilityStatus, Pageable pageable);

	@Query("SELECT p FROM ParkingSpace p WHERE p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true")
	Page<ParkingSpace> findAvailableParkingSpaces(Pageable pageable);

	@Query("SELECT p FROM ParkingSpace p WHERE p.slotType = :slotType AND p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true")
	Page<ParkingSpace> findAvailableBySlotType(@Param("slotType") SlotType slotType, Pageable pageable);

	@Query("SELECT p FROM ParkingSpace p WHERE p.location = :location AND p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true")
	Page<ParkingSpace> findAvailableByLocation(@Param("location") String location, Pageable pageable);

	/** Free, in-service spaces per slot type, as [SlotType, count] rows (types with none are absent). */
	@Query("SELECT p.slotType, COUNT(p) FROM ParkingSpace p WHERE p.availabilityStatus = com.parkingSystem.enums.SpaceStatus.AVAILABLE AND p.isActive = true GROUP BY p.slotType")
	List<Object[]> countAvailableGroupedBySlotType();
}
