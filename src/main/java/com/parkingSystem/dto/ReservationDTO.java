package com.parkingSystem.dto;

import com.parkingSystem.enums.ReservationStatus;
import com.parkingSystem.model.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Reservation response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDTO {
    private Long reservationId;
    private Long userId;
    private Long parkingSpaceId;
    private LocalDateTime reservationTime;
    private Integer duration;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime fulfilledAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime updatedAt;
    /** Ticket created when the owner drove in (FULFILLED only). */
    private Long ticketId;

    public static ReservationDTO from(Reservation reservation) {
        return ReservationDTO.builder()
                .reservationId(reservation.getReservationId())
                .userId(reservation.getUser().getUserId())
                .parkingSpaceId(reservation.getParkingSpace().getParkingSpaceId())
                .reservationTime(reservation.getReservationTime())
                .duration(reservation.getDuration())
                .status(reservation.getStatus())
                .expiresAt(reservation.getExpiresAt())
                .fulfilledAt(reservation.getFulfilledAt())
                .cancelledAt(reservation.getCancelledAt())
                .updatedAt(reservation.getUpdatedAt())
                .ticketId(reservation.getTicketId())
                .build();
    }
}
