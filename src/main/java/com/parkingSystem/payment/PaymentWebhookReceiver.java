package com.parkingSystem.payment;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.service.IPaymentService;
import com.parkingSystem.service.IPaymentService.WebhookResult;

import lombok.AllArgsConstructor;

/**
 * Entry point for provider webhooks: verify the signature over the raw body first, only then parse it and
 * hand it to the payment service. Used by the public webhook endpoint and by the mock provider.
 */
@Component
@AllArgsConstructor
public class PaymentWebhookReceiver {

    private WebhookSigner signer;
    private ObjectMapper objectMapper;
    private IPaymentService paymentService;

    public WebhookResult receive(String rawBody, String timestampHeader, String signatureHeader) {
        signer.verify(timestampHeader, signatureHeader, rawBody);
        PaymentWebhookEvent event;
        try {
            event = objectMapper.readValue(rawBody, PaymentWebhookEvent.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Webhook body is not a valid payment event");
        }
        return paymentService.handleWebhook(event);
    }
}
