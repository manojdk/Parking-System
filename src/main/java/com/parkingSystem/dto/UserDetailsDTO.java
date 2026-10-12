package com.parkingSystem.dto;

import java.time.LocalDateTime;
import java.util.Date;

import com.parkingSystem.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User profile response. Never contains the password hash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailsDTO {
    private Long userId;
    private String userName;
    private String userEmail;
    private String role;
    private String licensePlate;
    private Date registrationDate;
    private LocalDateTime createdDate;

    public static UserDetailsDTO from(User user) {
        return UserDetailsDTO.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userEmail(user.getUserEmail())
                .role(user.getRole())
                .licensePlate(user.getLicencePlate())
                .registrationDate(user.getRegistrationDate())
                .createdDate(user.getCreatedDate())
                .build();
    }
}
