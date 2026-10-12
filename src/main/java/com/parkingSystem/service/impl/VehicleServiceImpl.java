package com.parkingSystem.service.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.parkingSystem.enums.VehicleType;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.User;
import com.parkingSystem.model.Vehicle;
import com.parkingSystem.repository.UserRepository;
import com.parkingSystem.repository.VehicleRepository;
import com.parkingSystem.service.IVehicleService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class VehicleServiceImpl implements IVehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Vehicle registerVehicle(Long userId, String licensePlate, VehicleType vehicleType,
                                   String vehicleModel, String vehicleColor, String registrationNumber) {
        log.info("Registering vehicle for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (vehicleRepository.findByLicensePlate(licensePlate).isPresent()) {
            throw new ConflictException("Vehicle with license plate " + licensePlate + " already exists.");
        }
        if (registrationNumber != null && vehicleRepository.findByRegistrationNumber(registrationNumber).isPresent()) {
            throw new ConflictException("Vehicle with registration number " + registrationNumber + " already exists.");
        }

        Vehicle vehicle = Vehicle.builder()
                .user(user)
                .licensePlate(licensePlate)
                .vehicleType(vehicleType)
                .vehicleModel(vehicleModel)
                .vehicleColor(vehicleColor)
                .registrationNumber(registrationNumber)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Optional<Vehicle> getVehicleById(Long vehicleId) {
        log.info("Fetching vehicle by ID: {}", vehicleId);
        return vehicleRepository.findById(vehicleId);
    }

    @Override
    public Optional<Vehicle> getVehicleByLicensePlate(String licensePlate) {
        log.info("Fetching vehicle by license plate: {}", licensePlate);
        return vehicleRepository.findByLicensePlate(licensePlate);
    }

    @Override
    public Page<Vehicle> getUserVehicles(Long userId, Pageable pageable) {
        log.info("Fetching all vehicles for user ID: {}", userId);
        return vehicleRepository.findByUserUserId(userId, pageable);
    }

    @Override
    @Transactional
    public Vehicle updateVehicle(Long vehicleId, String vehicleModel, String vehicleColor) {
        log.info("Updating vehicle ID: {}", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        // Only overwrite fields that were provided
        if (vehicleModel != null) {
            vehicle.setVehicleModel(vehicleModel);
        }
        if (vehicleColor != null) {
            vehicle.setVehicleColor(vehicleColor);
        }
        vehicle.setUpdatedAt(LocalDateTime.now());
        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public void deactivateVehicle(Long vehicleId) {
        log.info("Deactivating vehicle ID: {}", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));
        vehicle.setIsActive(false);
        vehicle.setUpdatedAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);
    }

    @Override
    public Page<Vehicle> getAllActiveVehicles(Pageable pageable) {
        log.info("Fetching all active vehicles");
        return vehicleRepository.findByIsActiveTrue(pageable);
    }
}
