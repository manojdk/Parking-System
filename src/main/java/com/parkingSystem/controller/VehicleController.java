package com.parkingSystem.controller;

import com.parkingSystem.service.VehicleService;
import com.parkingSystem.model.Vehicle;
import com.parkingSystem.dto.VehicleRegistrationRequest;
import com.parkingSystem.dto.VehicleDTO;
import com.parkingSystem.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Vehicle management
 */
@RestController
@RequestMapping("/api/vehicles")
@AllArgsConstructor
@Slf4j
public class VehicleController {

    private VehicleService vehicleService;

    /**
     * Register a new vehicle
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerVehicle(@RequestBody VehicleRegistrationRequest request) {
        try {
            log.info("Vehicle registration request received");
            Vehicle vehicle = vehicleService.registerVehicle(
                    request.getUserId(),
                    request.getLicensePlate(),
                    request.getVehicleType(),
                    request.getVehicleModel(),
                    request.getVehicleColor(),
                    request.getRegistrationNumber()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Vehicle registered successfully");
            response.put("vehicle", convertToDTO(vehicle));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error registering vehicle: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get vehicle details
     */
    @GetMapping("/{vehicleId}")
    public ResponseEntity<?> getVehicle(@PathVariable Long vehicleId) {
        try {
            Vehicle vehicle = vehicleService.getVehicleById(vehicleId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
            return ResponseEntity.ok(convertToDTO(vehicle));
        } catch (Exception e) {
            log.error("Error fetching vehicle: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get vehicle by license plate
     */
    @GetMapping("/license-plate/{licensePlate}")
    public ResponseEntity<?> getVehicleByLicensePlate(@PathVariable String licensePlate) {
        try {
            Vehicle vehicle = vehicleService.getVehicleByLicensePlate(licensePlate)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
            return ResponseEntity.ok(convertToDTO(vehicle));
        } catch (Exception e) {
            log.error("Error fetching vehicle: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get all vehicles for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserVehicles(@PathVariable Long userId) {
        try {
            List<Vehicle> vehicles = vehicleService.getUserVehicles(userId);
            List<VehicleDTO> dtos = vehicles.stream().map(this::convertToDTO).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", vehicles.size());
            response.put("vehicles", dtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user vehicles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Update vehicle
     */
    @PutMapping("/{vehicleId}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long vehicleId, 
                                           @RequestParam(required = false) String vehicleModel,
                                           @RequestParam(required = false) String vehicleColor) {
        try {
            Vehicle vehicle = vehicleService.updateVehicle(vehicleId, vehicleModel, vehicleColor);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Vehicle updated successfully");
            response.put("vehicle", convertToDTO(vehicle));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating vehicle: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Deactivate vehicle
     */
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<?> deactivateVehicle(@PathVariable Long vehicleId) {
        try {
            vehicleService.deactivateVehicle(vehicleId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Vehicle deactivated successfully"));
        } catch (Exception e) {
            log.error("Error deactivating vehicle: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get all active vehicles
     */
    @GetMapping("/all/active")
    public ResponseEntity<?> getAllActiveVehicles() {
        try {
            List<Vehicle> vehicles = vehicleService.getAllActiveVehicles();
            List<VehicleDTO> dtos = vehicles.stream().map(this::convertToDTO).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", vehicles.size());
            response.put("vehicles", dtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching active vehicles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Helper method to convert Vehicle to DTO
     */
    private VehicleDTO convertToDTO(Vehicle vehicle) {
        return VehicleDTO.builder()
                .vehicleId(vehicle.getVehicleId())
                .userId(vehicle.getUser().getUserId())
                .licensePlate(vehicle.getLicensePlate())
                .vehicleType(vehicle.getVehicleType())
                .vehicleModel(vehicle.getVehicleModel())
                .vehicleColor(vehicle.getVehicleColor())
                .registrationNumber(vehicle.getRegistrationNumber())
                .isActive(vehicle.getIsActive())
                .build();
    }
}

