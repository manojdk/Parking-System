package com.parkingSystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.ParkingSpace;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

	@Query("SELECT p FROM ParkingSpace p WHERE p.availabilityStatus = true")
	List<ParkingSpace> findAvailableParkingSpaces();

	Optional<ParkingSpace> findByParkingSpaceId(Long parkingSpaceId);

	void deleteByParkingSpaceId(Long parkingSpaceId);
	
	
}
