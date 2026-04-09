package com.megawiki.integration.slack;

import static org.assertj.core.api.Assertions.assertThat;

import com.megawiki.config.SlackProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlackSignatureVerifierTest {

    private final SlackProperties slackProperties = new SlackProperties();
    private SlackSignatureVerifier slackSignatureVerifier;

    @BeforeEach
    void setUp() {
        slackProperties.setEnabled(true);
        slackProperties.setSigningSecret("test-signing-secret");
        slackSignatureVerifier = new SlackSignatureVerifier(slackProperties);
    }

    @Test
    void acceptsValidSlackSignature() {
        String payload = "{\"type\":\"event_callback\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = "v0=" + hmacSha256("v0:" + timestamp + ":" + payload, slackProperties.getSigningSecret());

        assertThat(slackSignatureVerifier.isValid(timestamp, signature, payload)).isTrue();
    }

    @Test
    void rejectsInvalidSignature() {
        String payload = "{\"type\":\"event_callback\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        assertThat(slackSignatureVerifier.isValid(timestamp, "v0=invalid", payload)).isFalse();
    }

    private static String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : digest) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
