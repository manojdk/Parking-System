package com.parkingSystem.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkingSystem.enums.BillingStatus;
import com.parkingSystem.enums.PaymentMethod;
import com.parkingSystem.enums.PaymentStatus;
import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.Bill;
import com.parkingSystem.model.Payment;
import com.parkingSystem.payment.PaymentProvider;
import com.parkingSystem.payment.PaymentWebhookEvent;
import com.parkingSystem.repository.BillRepository;
import com.parkingSystem.repository.PaymentRepository;
import com.parkingSystem.service.IPaymentService;

import lombok.extern.slf4j.Slf4j;

/**
 * Online payments.
 * <p>
 * Flow: the user starts a payment (PENDING, a charge is created at the provider), pays on the provider's
 * checkout page, and the provider reports the result through a signed webhook. Only that webhook marks the
 * bill PAID.
 * <p>
 * Concurrency: every method that changes a payment locks the bill row first and the payment second, the same
 * order the billing service uses for cash payments and discounts. A bill can have at most one PENDING payment
 * (checked under the lock and backed by a unique guard column).
 */
@Service
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

	static final String CURRENCY = "INR";

	private final PaymentRepository paymentRepository;
	private final BillRepository billRepository;
	private final PaymentProvider paymentProvider;
	private final Clock clock;
	private final Duration pendingTimeout;

	public PaymentServiceImpl(PaymentRepository paymentRepository, BillRepository billRepository,
			PaymentProvider paymentProvider, Clock clock,
			@Value("${app.payments.pending-timeout-minutes:15}") long pendingTimeoutMinutes) {
		if (pendingTimeoutMinutes <= 0) {
			throw new IllegalStateException("app.payments.pending-timeout-minutes must be positive");
		}
		this.paymentRepository = paymentRepository;
		this.billRepository = billRepository;
		this.paymentProvider = paymentProvider;
		this.clock = clock;
		this.pendingTimeout = Duration.ofMinutes(pendingTimeoutMinutes);
	}

	@Override
	@Transactional
	public PaymentStart startPayment(Long userId, Long billId, PaymentMethod method, String idempotencyKey) {
		Bill bill = lockBill(billId);
		if (!bill.getUser().getUserId().equals(userId)) {
			throw new BadRequestException("Bill does not belong to this user");
		}

		// Same key again: return what the first request produced, never charge twice
		Optional<Payment> earlier = paymentRepository.findByUserUserIdAndIdempotencyKey(userId, idempotencyKey);
		if (earlier.isPresent()) {
			Payment payment = earlier.get();
			if (!payment.getBillId().equals(billId) || payment.getMethod() != method) {
				throw new ConflictException("Idempotency-Key was already used for a different payment request");
			}
			return new PaymentStart(payment, true);
		}

		if (bill.getStatus() != BillingStatus.PENDING) {
			throw new ConflictException("Bill cannot be paid. Current status: " + bill.getStatus());
		}
		if (hasPaymentInProgress(billId)) {
			throw new ConflictException("A payment for this bill is already in progress");
		}

		LocalDateTime now = now();
		Payment payment = new Payment();
		payment.setBill(bill);
		payment.setBillId(billId);
		payment.setUser(bill.getUser());
		payment.setUserId(userId);
		payment.setAmount(bill.getTotalAmount());
		payment.setCurrency(CURRENCY);
		payment.setMethod(method);
		payment.setStatus(PaymentStatus.PENDING);
		payment.setProvider(paymentProvider.name());
		payment.setIdempotencyKey(idempotencyKey);
		payment.setCreatedAt(now);
		payment.setExpiresAt(now.plus(pendingTimeout));
		payment.setProviderReference(paymentProvider.createCharge(payment));

		Payment saved = paymentRepository.save(payment);
		log.info("Payment {} started for bill {} ({} {}), provider reference {}", saved.getPaymentId(), billId,
				saved.getAmount(), CURRENCY, saved.getProviderReference());
		return new PaymentStart(saved, false);
	}

	@Override
	@Transactional
	public WebhookResult handleWebhook(PaymentWebhookEvent event) {
		if (event == null || event.providerReference() == null || event.outcome() == null) {
			throw new BadRequestException("Webhook must contain providerReference and outcome");
		}
		String reference = event.providerReference();
		Long billId = paymentRepository.findBillIdByProviderReference(reference)
				.orElseThrow(() -> new ResourceNotFoundException("No payment with provider reference " + reference));
		Bill bill = lockBill(billId);
		Payment payment = paymentRepository.findByProviderReferenceForUpdate(reference)
				.orElseThrow(() -> new ResourceNotFoundException("No payment with provider reference " + reference));
		LocalDateTime now = now();

		return switch (event.outcome()) {
		case SUCCEEDED -> applySuccess(payment, bill, now);
		case FAILED -> applyFailure(payment, event.failureReason(), now);
		};
	}

	private WebhookResult applySuccess(Payment payment, Bill bill, LocalDateTime now) {
		switch (payment.getStatus()) {
		case SUCCEEDED, REFUND_REQUIRED:
			return WebhookResult.DUPLICATE;
		case FAILED:
			throw new ConflictException("Payment " + payment.getPaymentId() + " already failed; a success cannot follow");
		case PENDING, EXPIRED:
			break;
		}
		// The provider has the money. Settle the bill only if it is still open for exactly this amount;
		// otherwise (it was paid another way, or a late confirmation of an expired attempt) flag a refund.
		if (bill.getStatus() != BillingStatus.PENDING) {
			payment.requireRefund(now, "Bill was already " + bill.getStatus() + " when the payment succeeded");
			log.warn("Payment {} succeeded but bill {} is {}: refund required", payment.getPaymentId(),
					bill.getBillId(), bill.getStatus());
		} else if (bill.getTotalAmount().compareTo(payment.getAmount()) != 0) {
			payment.requireRefund(now, "Bill total changed from " + payment.getAmount() + " to " + bill.getTotalAmount());
			log.warn("Payment {} amount {} no longer matches bill {} total {}: refund required",
					payment.getPaymentId(), payment.getAmount(), bill.getBillId(), bill.getTotalAmount());
		} else {
			payment.succeed(now);
			bill.setStatus(BillingStatus.PAID);
			bill.setPaymentMethod(payment.getMethod().name());
			bill.setPaymentDate(now);
			billRepository.save(bill);
			log.info("Payment {} succeeded, bill {} is PAID", payment.getPaymentId(), bill.getBillId());
		}
		paymentRepository.save(payment);
		return WebhookResult.APPLIED;
	}

	private WebhookResult applyFailure(Payment payment, String reason, LocalDateTime now) {
		switch (payment.getStatus()) {
		case FAILED, EXPIRED:
			// Already closed without money moving; a late failure changes nothing
			return WebhookResult.DUPLICATE;
		case SUCCEEDED, REFUND_REQUIRED:
			throw new ConflictException("Payment " + payment.getPaymentId() + " already succeeded; a failure cannot follow");
		case PENDING:
			break;
		}
		payment.fail(now, reason == null || reason.isBlank() ? "Declined by the payment provider" : reason);
		paymentRepository.save(payment);
		log.info("Payment {} failed: {}", payment.getPaymentId(), payment.getFailureReason());
		return WebhookResult.APPLIED;
	}

	@Override
	@Transactional
	public boolean hasPaymentInProgress(Long billId) {
		Optional<Payment> open = paymentRepository.findByActiveBillId(billId);
		if (open.isEmpty()) {
			return false;
		}
		Payment payment = open.get();
		if (payment.isTimedOut(now())) {
			payment.expire(now());
			// Flushed now: a new payment for the same bill may be inserted next, and its insert would otherwise
			// reach the database before this update clears the unique guard column.
			paymentRepository.saveAndFlush(payment);
			log.info("Payment {} for bill {} expired without confirmation", payment.getPaymentId(), billId);
			return false;
		}
		return true;
	}

	@Override
	public Payment getPayment(Long paymentId) {
		return paymentRepository.findById(paymentId)
				.orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
	}

	@Override
	public Payment getPaymentByProviderReference(String providerReference) {
		return paymentRepository.findByProviderReference(providerReference)
				.orElseThrow(() -> new ResourceNotFoundException("No payment with provider reference " + providerReference));
	}

	@Override
	public List<Payment> getPaymentsForBill(Long billId) {
		return paymentRepository.findByBillBillIdOrderByCreatedAtDesc(billId);
	}

	private Bill lockBill(Long billId) {
		return billRepository.findByIdForUpdate(billId)
				.orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + billId));
	}

	/** Truncated to the database's precision, so values returned now equal the values read back later. */
	private LocalDateTime now() {
		return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
	}
}
