package com.parkingSystem.controller;

import org.springframework.data.domain.Sort;
import com.parkingSystem.dto.PageResponse;
import com.parkingSystem.controller.support.Pagination;
import com.parkingSystem.dto.VehicleDTO;
import com.parkingSystem.dto.VehicleRegistrationRequest;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.Vehicle;
import com.parkingSystem.security.CurrentUserService;
import com.parkingSystem.service.IVehicleService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Vehicle management
 */
@RestController
@RequestMapping("/api/vehicles")
@AllArgsConstructor
@Slf4j
public class VehicleController {

    private static final Sort BY_ID = Sort.by("vehicleId");

    private IVehicleService vehicleService;
    private CurrentUserService currentUserService;

    /**
     * Register a new vehicle
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerVehicle(@Valid @RequestBody VehicleRegistrationRequest request) {
        log.info("Vehicle registration request received");
        Long userId = currentUserService.resolveUserId(request.getUserId());
        Vehicle vehicle = vehicleService.registerVehicle(
                userId,
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
    }

    /**
     * Get vehicle details
     */
    @GetMapping("/{vehicleId}")
    public ResponseEntity<?> getVehicle(@PathVariable Long vehicleId) {
        Vehicle vehicle = findOwnedVehicle(vehicleId);
        return ResponseEntity.ok(convertToDTO(vehicle));
    }

    /**
     * Get vehicle by license plate
     */
    @GetMapping("/license-plate/{licensePlate}")
    public ResponseEntity<?> getVehicleByLicensePlate(@PathVariable String licensePlate) {
        Vehicle vehicle = vehicleService.getVehicleByLicensePlate(licensePlate)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        currentUserService.checkAccess(vehicle.getUser().getUserId());
        return ResponseEntity.ok(convertToDTO(vehicle));
    }

    /**
     * Get all vehicles for a user
     */
    @GetMapping("/user/{userId}")
    public PageResponse<VehicleDTO> getUserVehicles(@PathVariable Long userId,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        currentUserService.checkAccess(userId);
        return PageResponse.of(vehicleService.getUserVehicles(userId, Pagination.of(page, size, BY_ID)),
                this::convertToDTO);
    }

    /**
     * Update vehicle. Only the provided fields are changed.
     */
    @PutMapping("/{vehicleId}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long vehicleId,
                                           @RequestParam(required = false) String vehicleModel,
                                           @RequestParam(required = false) String vehicleColor) {
        findOwnedVehicle(vehicleId);
        Vehicle vehicle = vehicleService.updateVehicle(vehicleId, vehicleModel, vehicleColor);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Vehicle updated successfully");
        response.put("vehicle", convertToDTO(vehicle));
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate vehicle
     */
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<?> deactivateVehicle(@PathVariable Long vehicleId) {
        findOwnedVehicle(vehicleId);
        vehicleService.deactivateVehicle(vehicleId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Vehicle deactivated successfully"));
    }

    /**
     * Get all active vehicles (admin only)
     */
    @GetMapping("/all/active")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<VehicleDTO> getAllActiveVehicles(
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        return PageResponse.of(vehicleService.getAllActiveVehicles(Pagination.of(page, size, BY_ID)),
                this::convertToDTO);
    }

    /**
     * Loads a vehicle and checks that the caller owns it (or is an admin).
     */
    private Vehicle findOwnedVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleService.getVehicleById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));
        currentUserService.checkAccess(vehicle.getUser().getUserId());
        return vehicle;
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
