package com.parkingSystem.uuidTest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.enums.SlotType;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

	List<ParkingSpace> findByAvailabilityStatus(String availabilityStatus);

	Optional<ParkingSpace> findByParkingSpaceId(Long parkingSpaceId);

	void deleteByParkingSpaceId(Long parkingSpaceId);

    @Query("SELECT p FROM ParkingSpace p WHERE p.availabilityStatus = 'available' AND p.isActive = true")
	List<ParkingSpace> findAvailableParkingSpaces();

	List<ParkingSpace> findAll();

	// Slot type queries
	List<ParkingSpace> findBySlotType(SlotType slotType);

	@Query("SELECT p FROM ParkingSpace p WHERE p.slotType = :slotType AND p.availabilityStatus = 'available' AND p.isActive = true")
	List<ParkingSpace> findAvailableBySlotType(@Param("slotType") SlotType slotType);

	// Location-based queries
	@Query("SELECT p FROM ParkingSpace p WHERE p.location = :location AND p.availabilityStatus = 'available' AND p.isActive = true")
	List<ParkingSpace> findAvailableByLocation(@Param("location") String location);

	// Smart slot allocation - find nearest available slot
	@Query("SELECT p FROM ParkingSpace p WHERE p.location = :location AND p.slotType = :slotType AND p.availabilityStatus = 'available' AND p.isActive = true ORDER BY p.floorNumber ASC LIMIT 1")
	Optional<ParkingSpace> findNearestAvailableSlot(@Param("location") String location, @Param("slotType") SlotType slotType);

	// Count available slots by type
	@Query("SELECT COUNT(p) FROM ParkingSpace p WHERE p.slotType = :slotType AND p.availabilityStatus = 'available' AND p.isActive = true")
	Long countAvailableBySlotType(@Param("slotType") SlotType slotType);

	// Get all available slots count
	@Query("SELECT COUNT(p) FROM ParkingSpace p WHERE p.availabilityStatus = 'available' AND p.isActive = true")
	Long countAllAvailable();

	List<ParkingSpace> findByIsActiveTrueOrderByFloorNumber();

	@Query("SELECT p FROM ParkingSpace p WHERE p.location = :location AND p.slotType = :slotType AND p.availabilityStatus = 'available' AND p.isActive = true ORDER BY p.floorNumber ASC")
    List<ParkingSpace> findNearestAvailableSlots(@Param("location") String location, @Param("slotType") SlotType slotType);
}
