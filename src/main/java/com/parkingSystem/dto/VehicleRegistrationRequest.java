package com.parkingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.parkingSystem.enums.VehicleType;

/**
 * DTO for Vehicle registration request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRegistrationRequest {
    private Long userId;
    private String licensePlate;
    private VehicleType vehicleType;
    private String vehicleModel;
    private String vehicleColor;
    private String registrationNumber;
}

