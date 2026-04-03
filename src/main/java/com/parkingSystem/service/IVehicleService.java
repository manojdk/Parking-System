package com.parkingSystem.service;

import com.parkingSystem.model.Vehicle;
import com.parkingSystem.enums.VehicleType;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing vehicles
 */
public interface IVehicleService {

    /**
     * Register a new vehicle for a user
     */
    Vehicle registerVehicle(Long userId, String licensePlate, VehicleType vehicleType,
                            String vehicleModel, String vehicleColor, String registrationNumber);

    /**
     * Get vehicle by ID
     */
    Optional<Vehicle> getVehicleById(Long vehicleId);

    /**
     * Get vehicle by license plate
     */
    Optional<Vehicle> getVehicleByLicensePlate(String licensePlate);

    /**
     * Get all vehicles for a user
     */
    List<Vehicle> getUserVehicles(Long userId);

    /**
     * Update vehicle details
     */
    Vehicle updateVehicle(Long vehicleId, String vehicleModel, String vehicleColor);

    /**
     * Deactivate vehicle
     */
    void deactivateVehicle(Long vehicleId);

    /**
     * Get all active vehicles
     */
    List<Vehicle> getAllActiveVehicles();
}
