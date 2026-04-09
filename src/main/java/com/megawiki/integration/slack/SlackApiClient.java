package com.megawiki.integration.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.megawiki.config.SlackProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class SlackApiClient {

    private final RestClient restClient;
    private final SlackProperties slackProperties;

    public SlackApiClient(RestClient.Builder restClientBuilder, SlackProperties slackProperties) {
        this.slackProperties = slackProperties;
        this.restClient = restClientBuilder
                .baseUrl("https://slack.com/api")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void postThreadReply(String channel, String threadTs, String text) {
        if (!slackProperties.isEnabled() || !StringUtils.hasText(slackProperties.getBotToken())) {
            throw new IllegalStateException("Slack integration is enabled without a bot token.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", channel);
        body.put("text", text);
        body.put("mrkdwn", true);
        body.put("unfurl_links", false);
        body.put("unfurl_media", false);
        if (StringUtils.hasText(threadTs)) {
            body.put("thread_ts", threadTs);
        }

        JsonNode response = restClient.post()
                .uri("/chat.postMessage")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + slackProperties.getBotToken())
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("ok").asBoolean(false)) {
            String error = response == null ? "empty_response" : response.path("error").asText("unknown_error");
            throw new IllegalStateException("Slack chat.postMessage failed: " + error);
        }
    }
}
