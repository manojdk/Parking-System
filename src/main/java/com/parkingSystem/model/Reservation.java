package com.parkingSystem.model;
import java.time.LocalDateTime;

import com.parkingSystem.enums.ReservationStatus;
import com.parkingSystem.exception.ConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * A hold on a parking space for one user. It starts ACTIVE and ends exactly once as FULFILLED, CANCELLED
 * or EXPIRED. Rows are never deleted, so feedback can keep referring to them.
 */
@Entity
@Table(name = "parking_reservations")
@Data
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "parking_space_id", nullable = false)
    private ParkingSpace parkingSpace;

    /** When the reservation was made. */
    @Column(name = "reservation_time", nullable = false)
    private LocalDateTime reservationTime;

    /** Expected length of stay in hours, as given by the driver. Informational; billing uses the ticket. */
    @Column(name = "duration")
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    /** The space is held until this moment; after it the reservation is a no-show. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "fulfilled_at")
    private LocalDateTime fulfilledAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** The parking ticket that fulfilled this reservation. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private ParkingTicket ticket;

    /** Read-only copy of the ticket id, so responses can show it without loading the ticket. */
    @Column(name = "ticket_id", insertable = false, updatable = false)
    private Long ticketId;

    /**
     * Space id while ACTIVE, otherwise null. Unique in the database, so a space can never have two open
     * reservations (see V4 migration). Maintained by {@link #syncGuards()}.
     */
    @Column(name = "active_space_id")
    private Long activeSpaceId;

    /** User id while ACTIVE, otherwise null; unique, so a user holds at most one space at a time. */
    @Column(name = "active_user_id")
    private Long activeUserId;

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    /** True when the hold has run out at {@code now}, whether or not the expiry job has run yet. */
    public boolean isHoldOver(LocalDateTime now) {
        return isActive() && !now.isBefore(expiresAt);
    }

    /** The owner drove in: close the reservation and link it to the ticket. */
    public void fulfil(ParkingTicket parkingTicket, LocalDateTime now) {
        requireActive();
        this.status = ReservationStatus.FULFILLED;
        this.fulfilledAt = now;
        this.ticket = parkingTicket;
        this.ticketId = parkingTicket.getTicketId();
    }

    public void cancel(LocalDateTime now) {
        requireActive();
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = now;
    }

    /** No-show: the hold ran out. */
    public void expire() {
        requireActive();
        this.status = ReservationStatus.EXPIRED;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new ConflictException("Reservation is no longer active. Current status: " + status);
        }
    }

    @PrePersist
    public void prePersist() {
        this.updatedAt = this.reservationTime;
        syncGuards();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncGuards();
    }

    void syncGuards() {
        boolean active = isActive();
        this.activeSpaceId = active && parkingSpace != null ? parkingSpace.getParkingSpaceId() : null;
        this.activeUserId = active && user != null ? user.getUserId() : null;
    }
}
