package com.parkingSystem.payment;

import com.parkingSystem.model.Payment;

/**
 * The external payment provider (Razorpay, Stripe, ...). This project ships only {@link MockPaymentProvider};
 * a real provider would implement the same interface and send the same signed webhook.
 */
public interface PaymentProvider {

    /** Short name stored with each payment, e.g. "MOCK". */
    String name();

    /**
     * Registers a charge for the payment at the provider.
     *
     * @return the provider's reference for the charge
     */
    String createCharge(Payment payment);

    /** Where the user completes the payment (the provider's checkout page). */
    String checkoutUrl(String providerReference);
}
