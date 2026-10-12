package com.parkingSystem.payment;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parkingSystem.model.Payment;

/**
 * Stand-in for a real payment provider. Charges get an unguessable reference; the "checkout page" is
 * {@code MockProviderController}, which plays the provider's part by sending a signed webhook.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

    public static final String NAME = "MOCK";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String createCharge(Payment payment) {
        return "mock_ch_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String checkoutUrl(String providerReference) {
        return "/api/mock-provider/checkout/" + providerReference;
    }
}
