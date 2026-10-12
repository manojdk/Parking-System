package com.parkingSystem.dto;

import com.parkingSystem.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Start paying a bill. The request must also carry an {@code Idempotency-Key} header.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    @NotNull(message = "billId is mandatory")
    private Long billId;

    @NotNull(message = "method is mandatory (CARD, UPI or NET_BANKING)")
    private PaymentMethod method;
}
