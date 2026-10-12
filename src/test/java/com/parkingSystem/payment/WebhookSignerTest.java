package com.parkingSystem.payment;

import com.parkingSystem.exception.InvalidSignatureException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSignerTest {

    private static final String SECRET = "test-only-webhook-secret-0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final String BODY = "{\"providerReference\":\"mock_ch_1\",\"outcome\":\"SUCCEEDED\"}";

    private final WebhookSigner signer = new WebhookSigner(SECRET, 300, Clock.fixed(NOW, ZoneOffset.UTC));
    private final long now = NOW.getEpochSecond();

    @Test
    void validSignatureIsAccepted() {
        String signature = signer.sign(now, BODY);

        assertThat(signature).hasSize(64).matches("[0-9a-f]+");
        assertThatCode(() -> signer.verify(String.valueOf(now), signature, BODY)).doesNotThrowAnyException();
        assertThatCode(() -> signer.verify(String.valueOf(now), signature.toUpperCase(), BODY))
                .as("hex case does not matter").doesNotThrowAnyException();
    }

    @Test
    void changedBodyIsRejected() {
        String signature = signer.sign(now, BODY);

        assertThatThrownBy(() -> signer.verify(String.valueOf(now), signature, BODY.replace("SUCCEEDED", "FAILED")))
                .isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    void signatureFromAnotherSecretIsRejected() {
        WebhookSigner attacker = new WebhookSigner("some-other-secret-that-is-32-bytes-long",
                300, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> signer.verify(String.valueOf(now), attacker.sign(now, BODY), BODY))
                .isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    void timestampIsPartOfTheSignature() {
        String signature = signer.sign(now, BODY);

        assertThatThrownBy(() -> signer.verify(String.valueOf(now - 1), signature, BODY))
                .isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    void oldOrFutureWebhooksAreRejectedSoTheyCannotBeReplayed() {
        long old = now - 301;
        long future = now + 301;

        assertThatThrownBy(() -> signer.verify(String.valueOf(old), signer.sign(old, BODY), BODY))
                .isInstanceOf(InvalidSignatureException.class).hasMessageContaining("window");
        assertThatThrownBy(() -> signer.verify(String.valueOf(future), signer.sign(future, BODY), BODY))
                .isInstanceOf(InvalidSignatureException.class);
        long recent = now - 299;
        assertThatCode(() -> signer.verify(String.valueOf(recent), signer.sign(recent, BODY), BODY))
                .doesNotThrowAnyException();
    }

    @Test
    void missingOrMalformedHeadersAreRejected() {
        String signature = signer.sign(now, BODY);

        assertThatThrownBy(() -> signer.verify(null, signature, BODY)).isInstanceOf(InvalidSignatureException.class);
        assertThatThrownBy(() -> signer.verify(String.valueOf(now), null, BODY)).isInstanceOf(InvalidSignatureException.class);
        assertThatThrownBy(() -> signer.verify("yesterday", signature, BODY)).isInstanceOf(InvalidSignatureException.class);
        assertThatThrownBy(() -> signer.verify(String.valueOf(now), "not-hex", BODY))
                .isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    void shortSecretFailsAtStartup() {
        assertThatThrownBy(() -> new WebhookSigner("too-short", 300, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
