package com.parkingSystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // ---- paginated (step 6)

    Page<Vehicle> findByUserUserId(Long userId, Pageable pageable);

    Page<Vehicle> findByIsActiveTrue(Pageable pageable);
}
