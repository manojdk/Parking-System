package com.parkingSystem.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkingSystem.dto.PaymentDTO;
import com.parkingSystem.model.Payment;
import com.parkingSystem.payment.PaymentProvider;
import com.parkingSystem.payment.PaymentWebhookEvent;
import com.parkingSystem.payment.PaymentWebhookEvent.Outcome;
import com.parkingSystem.payment.PaymentWebhookReceiver;
import com.parkingSystem.payment.WebhookSigner;
import com.parkingSystem.security.CurrentUserService;
import com.parkingSystem.service.IPaymentService;
import com.parkingSystem.service.IPaymentService.WebhookResult;

import lombok.AllArgsConstructor;

/**
 * Plays the payment provider's checkout page, for demos and tests. "Paying" here makes the mock provider
 * send a signed webhook, which goes through exactly the same verification as a real one would at
 * {@code POST /api/payments/webhook}. Disable with {@code app.payments.mock.enabled=false}
 * (PAYMENT_MOCK_ENABLED) when a real provider is used.
 */
@RestController
@RequestMapping("/api/mock-provider")
@ConditionalOnProperty(name = "app.payments.mock.enabled", havingValue = "true", matchIfMissing = true)
@AllArgsConstructor
public class MockProviderController {

	private IPaymentService paymentService;
	private CurrentUserService currentUserService;
	private PaymentProvider paymentProvider;
	private WebhookSigner signer;
	private PaymentWebhookReceiver webhookReceiver;
	private ObjectMapper objectMapper;

	/**
	 * Complete the charge with the given outcome (default SUCCEEDED). Only the payer or an admin.
	 */
	@PostMapping("/checkout/{providerReference}")
	public ResponseEntity<Map<String, Object>> checkout(@PathVariable String providerReference,
			@RequestParam(defaultValue = "SUCCEEDED") Outcome outcome,
			@RequestParam(required = false) String failureReason) throws JsonProcessingException {
		Payment payment = paymentService.getPaymentByProviderReference(providerReference);
		currentUserService.checkAccess(payment.getUserId());

		String body = objectMapper.writeValueAsString(new PaymentWebhookEvent(providerReference, outcome, failureReason));
		long timestamp = signer.currentTimestamp();
		WebhookResult result = webhookReceiver.receive(body, String.valueOf(timestamp), signer.sign(timestamp, body));

		Payment after = paymentService.getPayment(payment.getPaymentId());
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("webhookResult", result);
		response.put("payment", PaymentDTO.from(after, paymentProvider.checkoutUrl(providerReference)));
		return ResponseEntity.ok(response);
	}
}
