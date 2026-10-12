package com.parkingSystem.payment;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.parkingSystem.exception.InvalidSignatureException;

/**
 * Signs and verifies provider webhooks, in the style real providers use (e.g. Stripe):
 * {@code signature = hex(HMAC-SHA256(secret, timestamp + "." + body))}.
 * <ul>
 * <li>The signature covers the exact raw body, so any change to it is detected.</li>
 * <li>The timestamp is signed too and must be recent, so a captured webhook cannot be replayed later.</li>
 * <li>Signatures are compared in constant time, so timing does not leak how many characters matched.</li>
 * </ul>
 */
@Component
public class WebhookSigner {

    public static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    public static final String TIMESTAMP_HEADER = "X-Webhook-Timestamp";
    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;
    private final Duration tolerance;
    private final Clock clock;

    public WebhookSigner(@Value("${app.payments.webhook-secret}") String secret,
            @Value("${app.payments.webhook-tolerance-seconds:300}") long toleranceSeconds, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.payments.webhook-secret (PAYMENT_WEBHOOK_SECRET) must be at least 32 bytes");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        this.tolerance = Duration.ofSeconds(toleranceSeconds);
        this.clock = clock;
    }

    /** Current time in epoch seconds, as sent in {@link #TIMESTAMP_HEADER}. */
    public long currentTimestamp() {
        return clock.instant().getEpochSecond();
    }

    public String sign(long timestamp, String body) {
        return HexFormat.of().formatHex(hmac(timestamp + "." + body));
    }

    /**
     * @throws InvalidSignatureException if a header is missing, the timestamp is too old or in the future, or
     *                                   the signature does not match the body
     */
    public void verify(String timestampHeader, String signatureHeader, String body) {
        if (timestampHeader == null || timestampHeader.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            throw new InvalidSignatureException("Missing webhook signature headers");
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException e) {
            throw new InvalidSignatureException("Invalid webhook timestamp");
        }
        long age = Math.abs(currentTimestamp() - timestamp);
        if (age > tolerance.getSeconds()) {
            throw new InvalidSignatureException("Webhook timestamp is outside the allowed window");
        }
        byte[] expected = hmac(timestamp + "." + (body == null ? "" : body));
        byte[] given;
        try {
            given = HexFormat.of().parseHex(signatureHeader.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidSignatureException("Invalid webhook signature");
        }
        if (!MessageDigest.isEqual(expected, given)) {
            throw new InvalidSignatureException("Invalid webhook signature");
        }
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
