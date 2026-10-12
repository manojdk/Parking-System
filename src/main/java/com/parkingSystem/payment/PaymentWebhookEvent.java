package com.parkingSystem.payment;

/**
 * Body of the provider's webhook: the final result of one charge.
 *
 * @param providerReference the charge, as returned by {@link PaymentProvider#createCharge}
 * @param outcome           SUCCEEDED or FAILED
 * @param failureReason     optional, for FAILED
 */
public record PaymentWebhookEvent(String providerReference, Outcome outcome, String failureReason) {

    public enum Outcome {
        SUCCEEDED,
        FAILED
    }
}
