package com.parkingSystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user update request. Null fields are left unchanged.
 * The role cannot be changed through this request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @NotNull(message = "userId is mandatory")
    private Long userId;

    @Size(min = 1, max = 50)
    private String userName;

    @Email(message = "Email must be valid")
    private String userEmail;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    private String licensePlate;
}
