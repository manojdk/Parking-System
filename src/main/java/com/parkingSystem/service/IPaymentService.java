package com.parkingSystem.service;

import java.util.List;

import com.parkingSystem.enums.PaymentMethod;
import com.parkingSystem.model.Payment;
import com.parkingSystem.payment.PaymentWebhookEvent;

/**
 * Online payment of bills through the payment provider.
 */
public interface IPaymentService {

    /**
     * Result of starting a payment.
     *
     * @param payment  the payment
     * @param replayed true if the idempotency key had been used before and the existing payment is returned
     */
    record PaymentStart(Payment payment, boolean replayed) {
    }

    /** What a webhook delivery did. */
    enum WebhookResult {
        /** The payment (and possibly the bill) changed. */
        APPLIED,
        /** The same result was already recorded; nothing changed (providers deliver webhooks more than once). */
        DUPLICATE
    }

    /**
     * Start paying a PENDING bill. Repeating the call with the same idempotency key returns the same payment.
     *
     * @param userId         the payer, who must own the bill
     * @param billId         bill to pay
     * @param method         payment method
     * @param idempotencyKey client-chosen key identifying this request
     */
    PaymentStart startPayment(Long userId, Long billId, PaymentMethod method, String idempotencyKey);

    /** Apply the provider's verified result for a charge. */
    WebhookResult handleWebhook(PaymentWebhookEvent event);

    /**
     * For the billing service, called while it holds the bill's row lock: expires a payment for the bill that
     * has timed out, and reports whether one is still in progress.
     */
    boolean hasPaymentInProgress(Long billId);

    Payment getPayment(Long paymentId);

    Payment getPaymentByProviderReference(String providerReference);

    List<Payment> getPaymentsForBill(Long billId);
}
