package com.parkingSystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.parkingSystem.enums.PaymentMethod;
import com.parkingSystem.enums.PaymentStatus;

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
 * One attempt to pay a bill through the payment provider. Only a verified provider webhook moves it out of
 * PENDING; the bill is marked PAID at the same moment.
 */
@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    /** Read-only copy of the bill id, usable without loading the bill. */
    @Column(name = "bill_id", insertable = false, updatable = false)
    private Long billId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Read-only copy of the payer's id. */
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    /** The bill total at the moment the payment was started. */
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    /** The provider's id for this charge; webhooks refer to the payment by it. */
    @Column(name = "provider_reference", nullable = false, length = 64)
    private String providerReference;

    /** Client-chosen key; unique per user, so a retried request returns this payment instead of charging again. */
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** If no confirmation has arrived by then, the attempt counts as EXPIRED and a new one is allowed. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Bill id while PENDING, otherwise null; unique, so a bill has at most one payment in progress (V5). */
    @Column(name = "active_bill_id")
    private Long activeBillId;

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    /** True when the attempt is still PENDING but its confirmation window has passed. */
    public boolean isTimedOut(LocalDateTime now) {
        return isPending() && !now.isBefore(expiresAt);
    }

    public void succeed(LocalDateTime now) {
        this.status = PaymentStatus.SUCCEEDED;
        this.completedAt = now;
        this.failureReason = null;
    }

    public void fail(LocalDateTime now, String reason) {
        this.status = PaymentStatus.FAILED;
        this.completedAt = now;
        this.failureReason = reason;
    }

    public void expire(LocalDateTime now) {
        this.status = PaymentStatus.EXPIRED;
        this.completedAt = now;
        this.failureReason = "No confirmation from the payment provider before " + expiresAt;
    }

    public void requireRefund(LocalDateTime now, String reason) {
        this.status = PaymentStatus.REFUND_REQUIRED;
        this.completedAt = now;
        this.failureReason = reason;
    }

    @PrePersist
    public void prePersist() {
        this.updatedAt = this.createdAt;
        syncGuard();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncGuard();
    }

    void syncGuard() {
        Long id = bill != null ? bill.getBillId() : billId;
        this.activeBillId = isPending() ? id : null;
    }
}
