package com.parkingSystem.integration;

import com.jayway.jsonpath.JsonPath;
import com.parkingSystem.payment.WebhookSigner;
import com.parkingSystem.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Online payments over HTTP: idempotent start, the mock provider's signed webhook, and the edge cases around
 * timeouts, duplicates, late confirmations and admin actions on the same bill. Pending timeout is 15 minutes.
 */
class PaymentIntegrationTest extends IntegrationTestSupport {

    private static final Duration PAST_THE_TIMEOUT = Duration.ofMinutes(16);

    @Autowired
    private WebhookSigner signer;

    // ---------------------------------------------------------------- happy path and idempotency

    @Test
    void billIsPaidOnlyWhenTheProviderConfirms() throws Exception {
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);

        MvcResult started = startPayment(alice, bill.id, "UPI", newKey())
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(11.0))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.providerReference", startsWith("mock_ch_")))
                .andExpect(jsonPath("$.checkoutUrl", startsWith("/api/mock-provider/checkout/")))
                .andReturn();
        String reference = JsonPath.read(body(started), "$.providerReference");

        billStatus(alice, bill).andExpect(jsonPath("$.status").value("PENDING"));

        checkout(alice, reference, "SUCCEEDED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookResult").value("APPLIED"))
                .andExpect(jsonPath("$.payment.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.payment.checkoutUrl").doesNotExist());

        billStatus(alice, bill)
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("UPI"));
        startPayment(alice, bill.id, "UPI", newKey()).andExpect(status().isConflict());
    }

    @Test
    void retryingWithTheSameKeyReturnsTheSamePaymentInsteadOfChargingAgain() throws Exception {
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);
        String key = newKey();

        Long first = longAt(startPayment(alice, bill.id, "CARD", key).andExpect(status().isCreated()).andReturn(),
                "$.paymentId");

        startPayment(alice, bill.id, "CARD", key)
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.paymentId").value(first));
        startPayment(alice, bill.id, "UPI", key)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Idempotency-Key")));
        paymentsForBill(alice, bill).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void idempotencyKeyIsRequiredAndValidated() throws Exception {
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);

        mockMvc.perform(post("/api/payments").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billId\":" + bill.id + ",\"method\":\"CARD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Idempotency-Key")));
        startPayment(alice, bill.id, "CARD", "short").andExpect(status().isBadRequest());
        startPayment(alice, bill.id, "CARD", "has spaces in it").andExpect(status().isBadRequest());
        startPayment(alice, bill.id, "BITCOIN", newKey()).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- who may pay

    @Test
    void usersCanNoLongerMarkTheirOwnBillPaidButAdminsCanRecordCash() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);

        mockMvc.perform(put("/api/billing/" + bill.id + "/pay").param("paymentMethod", "CARD")
                        .header("Authorization", alice.bearer()))
                .andExpect(status().isForbidden());
        billStatus(alice, bill).andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(put("/api/billing/" + bill.id + "/pay").param("paymentMethod", "CASH")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bill.status").value("PAID"));
    }

    @Test
    void onlyTheOwnerCanPayOrCompleteTheCheckout() throws Exception {
        Session alice = registerAndLogin();
        Session bob = registerAndLogin();
        Bill bill = pendingBill(alice);

        startPayment(bob, bill.id, "CARD", newKey()).andExpect(status().isForbidden());

        String reference = JsonPath.read(body(startPayment(alice, bill.id, "CARD", newKey()).andReturn()),
                "$.providerReference");
        checkout(bob, reference, "SUCCEEDED").andExpect(status().isForbidden());
        billStatus(alice, bill).andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ---------------------------------------------------------------- one payment at a time, timeouts

    @Test
    void secondPaymentWaitsUntilTheFirstTimesOut() throws Exception {
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);
        Long first = longAt(startPayment(alice, bill.id, "CARD", newKey()).andReturn(), "$.paymentId");

        startPayment(alice, bill.id, "UPI", newKey())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already in progress")));

        clock.advance(PAST_THE_TIMEOUT);
        startPayment(alice, bill.id, "UPI", newKey()).andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments/" + first).header("Authorization", alice.bearer()))
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.failureReason", containsString("No confirmation")));
    }

    @Test
    void lateConfirmationOfAnExpiredPaymentAfterTheBillWasPaidIsFlaggedForRefund() throws Exception {
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);
        String slowReference = JsonPath.read(body(startPayment(alice, bill.id, "CARD", newKey()).andReturn()),
                "$.providerReference");

        clock.advance(PAST_THE_TIMEOUT);
        String fastReference = JsonPath.read(body(startPayment(alice, bill.id, "UPI", newKey()).andReturn()),
                "$.providerReference");
        checkout(alice, fastReference, "SUCCEEDED").andExpect(jsonPath("$.payment.status").value("SUCCEEDED"));

        // The provider finally confirms the first, expired attempt: the money must go back, not pay twice
        sendWebhook(slowReference, "SUCCEEDED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));

        paymentsForBill(alice, bill)
                .andExpect(jsonPath("$[?(@.providerReference == '" + slowReference + "')].status").value("REFUND_REQUIRED"))
                .andExpect(jsonPath("$[?(@.providerReference == '" + fastReference + "')].status").value("SUCCEEDED"));
        billStatus(alice, bill).andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("UPI"));
    }

    @Test
    void lateConfirmationOfAnExpiredPaymentStillPaysAnOpenBill() throws Exception {
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);
        String reference = JsonPath.read(body(startPayment(alice, bill.id, "CARD", newKey()).andReturn()),
                "$.providerReference");
        clock.advance(PAST_THE_TIMEOUT);

        sendWebhook(reference, "SUCCEEDED").andExpect(status().isOk());

        billStatus(alice, bill).andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void failedPaymentLeavesTheBillOpenAndTheUserCanTryAgain() throws Exception {
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);
        String reference = JsonPath.read(body(startPayment(alice, bill.id, "CARD", newKey()).andReturn()),
                "$.providerReference");

        mockMvc.perform(post("/api/mock-provider/checkout/" + reference).param("outcome", "FAILED")
                        .param("failureReason", "Card declined").header("Authorization", alice.bearer()))
                .andExpect(jsonPath("$.payment.status").value("FAILED"))
                .andExpect(jsonPath("$.payment.failureReason").value("Card declined"));

        billStatus(alice, bill).andExpect(jsonPath("$.status").value("PENDING"));
        startPayment(alice, bill.id, "UPI", newKey()).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- the webhook endpoint

    @Test
    void webhookIsVerifiedAndSafeToDeliverMoreThanOnce() throws Exception {
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);
        String reference = JsonPath.read(body(startPayment(alice, bill.id, "CARD", newKey()).andReturn()),
                "$.providerReference");
        String event = event(reference, "SUCCEEDED");

        // No JWT is needed, but the signature is
        mockMvc.perform(post("/api/payments/webhook").contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isUnauthorized());
        long now = signer.currentTimestamp();
        mockMvc.perform(post("/api/payments/webhook").contentType(MediaType.APPLICATION_JSON).content(event)
                        .header(WebhookSigner.TIMESTAMP_HEADER, now)
                        .header(WebhookSigner.SIGNATURE_HEADER, signer.sign(now, event(reference, "FAILED"))))
                .andExpect(status().isUnauthorized());
        long old = now - 600;
        mockMvc.perform(post("/api/payments/webhook").contentType(MediaType.APPLICATION_JSON).content(event)
                        .header(WebhookSigner.TIMESTAMP_HEADER, old)
                        .header(WebhookSigner.SIGNATURE_HEADER, signer.sign(old, event)))
                .andExpect(status().isUnauthorized());
        billStatus(alice, bill).andExpect(jsonPath("$.status").value("PENDING"));

        sendWebhook(reference, "SUCCEEDED").andExpect(status().isOk()).andExpect(jsonPath("$.result").value("APPLIED"));
        sendWebhook(reference, "SUCCEEDED").andExpect(status().isOk()).andExpect(jsonPath("$.result").value("DUPLICATE"));
        sendWebhook(reference, "FAILED").andExpect(status().isConflict());
        billStatus(alice, bill).andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void webhookForAnUnknownChargeOrWithABrokenBodyIsRejected() throws Exception {
        sendWebhook("mock_ch_does_not_exist", "SUCCEEDED").andExpect(status().isNotFound());

        String garbage = "{not json";
        long now = signer.currentTimestamp();
        mockMvc.perform(post("/api/payments/webhook").contentType(MediaType.APPLICATION_JSON).content(garbage)
                        .header(WebhookSigner.TIMESTAMP_HEADER, now)
                        .header(WebhookSigner.SIGNATURE_HEADER, signer.sign(now, garbage)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- admin actions during a payment

    @Test
    void adminCannotChangeOrSettleABillWhileItIsBeingPaidOnline() throws Exception {
        String admin = createAdmin();
        Session alice = registerAndLogin();
        Bill bill = pendingBill(alice);
        String reference = JsonPath.read(body(startPayment(alice, bill.id, "CARD", newKey()).andReturn()),
                "$.providerReference");

        mockMvc.perform(put("/api/billing/" + bill.id + "/apply-discount").param("discountAmount", "1.00")
                        .header("Authorization", admin))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/billing/" + bill.id + "/pay").param("paymentMethod", "CASH")
                        .header("Authorization", admin))
                .andExpect(status().isConflict());

        checkout(alice, reference, "SUCCEEDED").andExpect(jsonPath("$.payment.status").value("SUCCEEDED"));
        billStatus(alice, bill).andExpect(jsonPath("$.totalAmount").value(11.0));
    }

    // ---------------------------------------------------------------- helpers

    private record Bill(Long id, String invoiceNumber) {
    }

    /** Parks and exits once, which creates a PENDING bill of 11.00 (1 hour at 10.00 + 10% tax). */
    private Bill pendingBill(Session owner) throws Exception {
        String admin = createAdmin();
        Long vehicleId = registerVehicle(owner);
        Long spaceId = createSpace(admin);
        String reference = JsonPath.read(body(enter(owner, vehicleId, spaceId).andExpect(status().isCreated())
                .andReturn()), "$.ticket.ticketReference");
        MvcResult exit = mockMvc.perform(post("/api/parking/exit").param("ticketReference", reference)
                        .header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andReturn();
        return new Bill(longAt(exit, "$.bill.billId"), JsonPath.read(body(exit), "$.bill.invoiceNumber"));
    }

    private static String newKey() {
        return "key-" + UUID.randomUUID();
    }

    private ResultActions startPayment(Session session, Long billId, String method, String key) throws Exception {
        return mockMvc.perform(post("/api/payments").header("Authorization", session.bearer())
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"billId\":" + billId + ",\"method\":\"" + method + "\"}"));
    }

    private ResultActions checkout(Session session, String reference, String outcome) throws Exception {
        return mockMvc.perform(post("/api/mock-provider/checkout/" + reference).param("outcome", outcome)
                .header("Authorization", session.bearer()));
    }

    /** Delivers a correctly signed webhook, as the provider would. */
    private ResultActions sendWebhook(String reference, String outcome) throws Exception {
        String event = event(reference, outcome);
        long now = signer.currentTimestamp();
        return mockMvc.perform(post("/api/payments/webhook").contentType(MediaType.APPLICATION_JSON).content(event)
                .header(WebhookSigner.TIMESTAMP_HEADER, now)
                .header(WebhookSigner.SIGNATURE_HEADER, signer.sign(now, event)));
    }

    private static String event(String reference, String outcome) {
        return "{\"providerReference\":\"" + reference + "\",\"outcome\":\"" + outcome + "\"}";
    }

    private ResultActions billStatus(Session session, Bill bill) throws Exception {
        return mockMvc.perform(get("/api/billing/invoice/" + bill.invoiceNumber).header("Authorization", session.bearer()))
                .andExpect(status().isOk());
    }

    private ResultActions paymentsForBill(Session session, Bill bill) throws Exception {
        return mockMvc.perform(get("/api/payments/bill/" + bill.id).header("Authorization", session.bearer()))
                .andExpect(status().isOk());
    }
}
