package com.parkingSystem.uuidTest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.ParkingSpace;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

	List<ParkingSpace> findByAvailabilityStatus(String availabilityStatus);

	Optional<ParkingSpace> findByParkingSpaceId(Long parkingSpaceId);

	void deleteByParkingSpaceId(Long parkingSpaceId);

    @Query("SELECT p FROM ParkingSpace p WHERE p.availabilityStatus = :availabilityStatus")
	List<ParkingSpace> findAvailableParkingSpaces();

	List<ParkingSpace> findAll();

}
