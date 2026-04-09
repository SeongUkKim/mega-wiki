package com.megawiki.integration.slack;

import com.megawiki.config.SlackProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SlackSignatureVerifier {

    private static final Duration MAX_REQUEST_AGE = Duration.ofMinutes(5);

    private final SlackProperties slackProperties;

    public SlackSignatureVerifier(SlackProperties slackProperties) {
        this.slackProperties = slackProperties;
    }

    public boolean isValid(String timestamp, String signature, String payload) {
        if (!slackProperties.isEnabled()
                || !StringUtils.hasText(slackProperties.getSigningSecret())
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(signature)
                || payload == null) {
            return false;
        }

        long requestEpochSeconds;
        try {
            requestEpochSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            return false;
        }

        Instant requestTime = Instant.ofEpochSecond(requestEpochSeconds);
        Instant now = Instant.now();
        if (requestTime.isBefore(now.minus(MAX_REQUEST_AGE)) || requestTime.isAfter(now.plus(Duration.ofMinutes(1)))) {
            return false;
        }

        String baseString = "v0:" + timestamp + ":" + payload;
        String expectedSignature = "v0=" + hmacSha256(baseString, slackProperties.getSigningSecret());
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
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
            throw new IllegalStateException("Failed to compute Slack signature", exception);
        }
    }
}
