package com.megawiki.integration.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.megawiki.config.SlackProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/slack")
public class SlackEventController {

    private final ObjectMapper objectMapper;
    private final SlackProperties slackProperties;
    private final SlackSignatureVerifier slackSignatureVerifier;
    private final SlackEventService slackEventService;

    public SlackEventController(
            ObjectMapper objectMapper,
            SlackProperties slackProperties,
            SlackSignatureVerifier slackSignatureVerifier,
            SlackEventService slackEventService
    ) {
        this.objectMapper = objectMapper;
        this.slackProperties = slackProperties;
        this.slackSignatureVerifier = slackSignatureVerifier;
        this.slackEventService = slackEventService;
    }

    @PostMapping("/events")
    public ResponseEntity<?> receiveEvent(
            @RequestHeader(value = "X-Slack-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Slack-Signature", required = false) String signature,
            @RequestBody String payload
    ) throws Exception {
        if (!slackProperties.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("Slack integration is disabled."));
        }

        if (!slackSignatureVerifier.isValid(timestamp, signature, payload)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Slack request signature is invalid."));
        }

        JsonNode request = objectMapper.readTree(payload);
        if ("url_verification".equals(request.path("type").asText())) {
            return ResponseEntity.ok(new ChallengeResponse(request.path("challenge").asText()));
        }

        slackEventService.acceptEvent(request);
        return ResponseEntity.ok(new AcknowledgementResponse(true));
    }

    private record ChallengeResponse(String challenge) {
    }

    private record AcknowledgementResponse(boolean ok) {
    }

    private record ErrorResponse(String error) {
    }
}
