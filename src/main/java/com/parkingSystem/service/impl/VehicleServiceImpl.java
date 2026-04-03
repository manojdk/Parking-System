package com.parkingSystem.service.impl;

import com.parkingSystem.enums.VehicleType;
import com.parkingSystem.model.User;
import com.parkingSystem.model.Vehicle;
import com.parkingSystem.repository.UserRepository;
import com.parkingSystem.repository.VehicleRepository;
import com.parkingSystem.service.IVehicleService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class VehicleServiceImpl implements IVehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    @Override
    public Vehicle registerVehicle(Long userId, String licensePlate, VehicleType vehicleType,
                                   String vehicleModel, String vehicleColor, String registrationNumber) {
        log.info("Registering vehicle for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (vehicleRepository.findByLicensePlate(licensePlate).isPresent()) {
            throw new IllegalArgumentException("Vehicle with license plate " + licensePlate + " already exists.");
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
    public List<Vehicle> getUserVehicles(Long userId) {
        log.info("Fetching all vehicles for user ID: {}", userId);
        return vehicleRepository.findByUserUserId(userId);
    }

    @Override
    public Vehicle updateVehicle(Long vehicleId, String vehicleModel, String vehicleColor) {
        log.info("Updating vehicle ID: {}", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        vehicle.setVehicleModel(vehicleModel);
        vehicle.setVehicleColor(vehicleColor);
        vehicle.setUpdatedAt(LocalDateTime.now());
        return vehicleRepository.save(vehicle);
    }

    @Override
    public void deactivateVehicle(Long vehicleId) {
        log.info("Deactivating vehicle ID: {}", vehicleId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));
        vehicle.setIsActive(false);
        vehicle.setUpdatedAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);
    }

    @Override
    public List<Vehicle> getAllActiveVehicles() {
        log.info("Fetching all active vehicles");
        return vehicleRepository.findByIsActiveTrue();
    }
}
