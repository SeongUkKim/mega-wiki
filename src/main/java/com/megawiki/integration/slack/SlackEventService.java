package com.megawiki.integration.slack;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SlackEventService {

    private final SlackApiClient slackApiClient;
    private final TaskExecutor taskExecutor;
    private final SlackEventDeduplicator deduplicator;
    private final List<SlackMentionProcessor> mentionProcessors;

    public SlackEventService(
            SlackApiClient slackApiClient,
            @Qualifier("slackEventExecutor") TaskExecutor taskExecutor,
            SlackEventDeduplicator deduplicator,
            List<SlackMentionProcessor> mentionProcessors
    ) {
        this.slackApiClient = slackApiClient;
        this.taskExecutor = taskExecutor;
        this.deduplicator = deduplicator;
        this.mentionProcessors = mentionProcessors;
    }

    public void acceptEvent(JsonNode payload) {
        if (!"event_callback".equals(payload.path("type").asText())) {
            return;
        }

        String eventId = payload.path("event_id").asText();
        if (!deduplicator.tryMarkProcessed(eventId)) {
            return;
        }

        JsonNode event = payload.path("event");
        if (!"app_mention".equals(event.path("type").asText()) || event.hasNonNull("bot_id")) {
            return;
        }

        taskExecutor.execute(() -> processMention(event, eventId));
    }

    private void processMention(JsonNode event, String eventId) {
        SlackMentionRequest request = new SlackMentionRequest(
                eventId,
                event.path("channel").asText(),
                event.path("thread_ts").asText(event.path("ts").asText()),
                event.path("user").asText(),
                normalizeQuestion(event.path("text").asText(""))
        );

        if (!StringUtils.hasText(request.question())) {
            slackApiClient.postThreadReply(
                    request.channel(),
                    request.threadTs(),
                    SlackReplyFormatter.questionRequired()
            );
            return;
        }

        resolveProcessor(request).process(request);
    }

    private SlackMentionProcessor resolveProcessor(SlackMentionRequest request) {
        return mentionProcessors.stream()
                .filter(processor -> processor.supports(request))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Slack mention processor is available."));
    }

    private static String normalizeQuestion(String rawText) {
        return rawText.replaceAll("<@[^>]+>", " ")
                .replace('&', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }
}