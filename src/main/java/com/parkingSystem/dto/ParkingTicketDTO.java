package com.parkingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.parkingSystem.enums.TicketStatus;
import java.time.LocalDateTime;

/**
 * DTO for Parking Ticket response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingTicketDTO {
    private Long ticketId;
    private String ticketReference;
    private Long userId;
    private Long vehicleId;
    private Long parkingSpaceId;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Long durationMinutes;
    private TicketStatus status;
    private String notes;
}

