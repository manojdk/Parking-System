package com.parkingSystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.parkingSystem.enums.PaymentMethod;
import com.parkingSystem.enums.PaymentStatus;
import com.parkingSystem.model.Payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment response. {@code checkoutUrl} is where the user completes the payment while it is PENDING.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private Long paymentId;
    private Long billId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private PaymentStatus status;
    private String provider;
    private String providerReference;
    private String checkoutUrl;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;

    public static PaymentDTO from(Payment payment, String checkoutUrl) {
        return PaymentDTO.builder()
                .paymentId(payment.getPaymentId())
                .billId(payment.getBillId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .provider(payment.getProvider())
                .providerReference(payment.getProviderReference())
                .checkoutUrl(payment.isPending() ? checkoutUrl : null)
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .expiresAt(payment.getExpiresAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }
}
