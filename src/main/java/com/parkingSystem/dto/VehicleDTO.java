package com.parkingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.parkingSystem.enums.VehicleType;

/**
 * DTO for Vehicle response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDTO {
    private Long vehicleId;
    private Long userId;
    private String licensePlate;
    private VehicleType vehicleType;
    private String vehicleModel;
    private String vehicleColor;
    private String registrationNumber;
    private Boolean isActive;
}

