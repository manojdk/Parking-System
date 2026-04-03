package com.parkingSystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.parkingSystem.enums.VehicleType;

/**
 * Vehicle model representing a user's vehicle in the parking system
 */
@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "license_plate", nullable = false, unique = true)
    @NotBlank(message = "License plate is mandatory")
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "vehicle_model")
    private String vehicleModel;

    @Column(name = "vehicle_color")
    private String vehicleColor;

    @Column(name = "registration_number", unique = true)
    private String registrationNumber;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

}

