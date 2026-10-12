package com.parkingSystem.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parkingSystem.dto.CreatePaymentRequest;
import com.parkingSystem.dto.PaymentDTO;
import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.model.Payment;
import com.parkingSystem.payment.PaymentProvider;
import com.parkingSystem.payment.PaymentWebhookReceiver;
import com.parkingSystem.payment.WebhookSigner;
import com.parkingSystem.security.CurrentUserService;
import com.parkingSystem.service.IBillingService;
import com.parkingSystem.service.IPaymentService;
import com.parkingSystem.service.IPaymentService.PaymentStart;
import com.parkingSystem.service.IPaymentService.WebhookResult;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

/**
 * Online payment of bills.
 * <ol>
 * <li>{@code POST /api/payments} with an {@code Idempotency-Key} header starts a payment and returns a
 * checkout URL.</li>
 * <li>The user pays at the provider (here: {@code /api/mock-provider/checkout/...}).</li>
 * <li>The provider calls {@code POST /api/payments/webhook} with a signed result; only then is the bill
 * PAID.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/payments")
@AllArgsConstructor
public class PaymentController {

	public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
	public static final String REPLAYED_HEADER = "Idempotent-Replayed";
	private static final Pattern KEY_FORMAT = Pattern.compile("[A-Za-z0-9_-]{8,100}");

	private IPaymentService paymentService;
	private IBillingService billingService;
	private CurrentUserService currentUserService;
	private PaymentProvider paymentProvider;
	private PaymentWebhookReceiver webhookReceiver;

	/**
	 * Start paying a bill. Only the bill's owner can pay it online. Repeating the request with the same
	 * Idempotency-Key returns the original payment (200, header {@code Idempotent-Replayed: true}).
	 */
	@PostMapping
	public ResponseEntity<PaymentDTO> startPayment(@RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
			@Valid @RequestBody CreatePaymentRequest request) {
		if (!KEY_FORMAT.matcher(idempotencyKey).matches()) {
			throw new BadRequestException(
					"Idempotency-Key must be 8-100 characters long and contain only letters, digits, '-' or '_'");
		}
		Long ownerId = billingService.getBillById(request.getBillId()).getUser().getUserId();
		Long callerId = currentUserService.getCurrentUser().getUserId();
		if (!ownerId.equals(callerId)) {
			throw new AccessDeniedException("Only the bill's owner can pay it online");
		}

		PaymentStart start = paymentService.startPayment(callerId, request.getBillId(), request.getMethod(),
				idempotencyKey);
		return ResponseEntity.status(start.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
				.header(REPLAYED_HEADER, String.valueOf(start.replayed()))
				.body(toDTO(start.payment()));
	}

	@GetMapping("/{paymentId}")
	public ResponseEntity<PaymentDTO> getPayment(@PathVariable Long paymentId) {
		Payment payment = paymentService.getPayment(paymentId);
		currentUserService.checkAccess(payment.getUserId());
		return ResponseEntity.ok(toDTO(payment));
	}

	/** All payment attempts for a bill, newest first. */
	@GetMapping("/bill/{billId}")
	public ResponseEntity<List<PaymentDTO>> getPaymentsForBill(@PathVariable Long billId) {
		currentUserService.checkAccess(billingService.getBillById(billId).getUser().getUserId());
		return ResponseEntity.ok(paymentService.getPaymentsForBill(billId).stream().map(this::toDTO).toList());
	}

	/**
	 * Called by the payment provider, not by users, so it needs no JWT. It is trusted only if the HMAC
	 * signature over the raw body and timestamp is valid (401 otherwise). Repeated deliveries are harmless.
	 */
	@PostMapping("/webhook")
	public ResponseEntity<Map<String, Object>> webhook(@RequestBody String rawBody,
			@RequestHeader(value = WebhookSigner.TIMESTAMP_HEADER, required = false) String timestamp,
			@RequestHeader(value = WebhookSigner.SIGNATURE_HEADER, required = false) String signature) {
		WebhookResult result = webhookReceiver.receive(rawBody, timestamp, signature);
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("received", true);
		response.put("result", result);
		return ResponseEntity.ok(response);
	}

	private PaymentDTO toDTO(Payment payment) {
		return PaymentDTO.from(payment, paymentProvider.checkoutUrl(payment.getProviderReference()));
	}
}
