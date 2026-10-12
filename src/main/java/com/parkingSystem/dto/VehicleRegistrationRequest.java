package com.parkingSystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.parkingSystem.enums.VehicleType;

/**
 * DTO for Vehicle registration request. If userId is omitted the authenticated user is used.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRegistrationRequest {
    private Long userId;
    @NotBlank(message = "licensePlate is mandatory")
    private String licensePlate;
    @NotNull(message = "vehicleType is mandatory")
    private VehicleType vehicleType;
    private String vehicleModel;
    private String vehicleColor;
    private String registrationNumber;
}

