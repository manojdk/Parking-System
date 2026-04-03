package com.parkingSystem.repository;

import com.parkingSystem.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Vehicle entity
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    List<Vehicle> findByUserUserId(Long userId);

    List<Vehicle> findByUserUserIdAndIsActiveTrue(Long userId);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    List<Vehicle> findByIsActiveTrue();
}

