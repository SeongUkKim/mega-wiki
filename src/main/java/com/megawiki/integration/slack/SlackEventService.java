package com.megawiki.integration.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.QuestionThread;
import com.megawiki.repository.KnowledgePageRepository;
import com.megawiki.service.QuestionWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SlackEventService {

    private static final Logger log = LoggerFactory.getLogger(SlackEventService.class);

    private final QuestionWorkflowService questionWorkflowService;
    private final KnowledgePageRepository knowledgePageRepository;
    private final SlackApiClient slackApiClient;
    private final TaskExecutor taskExecutor;
    private final SlackEventDeduplicator deduplicator;

    public SlackEventService(
            QuestionWorkflowService questionWorkflowService,
            KnowledgePageRepository knowledgePageRepository,
            SlackApiClient slackApiClient,
            @Qualifier("slackEventExecutor") TaskExecutor taskExecutor,
            SlackEventDeduplicator deduplicator
    ) {
        this.questionWorkflowService = questionWorkflowService;
        this.knowledgePageRepository = knowledgePageRepository;
        this.slackApiClient = slackApiClient;
        this.taskExecutor = taskExecutor;
        this.deduplicator = deduplicator;
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
        String channel = event.path("channel").asText();
        String threadTs = event.path("thread_ts").asText(event.path("ts").asText());
        String user = event.path("user").asText();
        String question = normalizeQuestion(event.path("text").asText(""));

        if (!StringUtils.hasText(question)) {
            slackApiClient.postThreadReply(channel, threadTs,
                    "Please add a question after mentioning the bot so Mega-Wiki can archive it.");
            return;
        }

        try {
            QuestionThread thread = questionWorkflowService.submitQuestion(
                    user,
                    channel,
                    question,
                    KnowledgeSourceType.SLACK_THREAD
            );
            KnowledgePage page = knowledgePageRepository.findById(thread.getLinkedPageId())
                    .orElseThrow(() -> new IllegalStateException("Linked knowledge page was not found: " + thread.getLinkedPageId()));
            slackApiClient.postThreadReply(channel, threadTs, buildSlackReply(page, thread));
        } catch (Exception exception) {
            log.error("Failed to process Slack event {}", eventId, exception);
            safePostFailure(channel, threadTs);
        }
    }

    private void safePostFailure(String channel, String threadTs) {
        try {
            slackApiClient.postThreadReply(
                    channel,
                    threadTs,
                    "Mega-Wiki could not finish the answer flow for this thread. Check the server logs and integration credentials."
            );
        } catch (Exception secondaryException) {
            log.error("Failed to publish Slack failure notice", secondaryException);
        }
    }

    private static String normalizeQuestion(String rawText) {
        return rawText.replaceAll("<@[^>]+>", " ")
                .replace('&', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String buildSlackReply(KnowledgePage page, QuestionThread thread) {
        return "*" + page.getTitle() + "*\n"
                + thread.getAiAnswer() + "\n\n"
                + "Saved in Mega-Wiki with page id `" + page.getId() + "`.";
    }
}
