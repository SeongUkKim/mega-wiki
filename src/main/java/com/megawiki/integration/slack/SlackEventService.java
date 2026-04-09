package com.megawiki.integration.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.megawiki.config.SnowflakeProperties;
import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.QuestionThread;
import com.megawiki.integration.snowflake.SnowflakeCortexClient;
import com.megawiki.integration.snowflake.SnowflakeCortexResponse;
import com.megawiki.repository.KnowledgePageRepository;
import com.megawiki.service.QuestionWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
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
    private final SnowflakeCortexClient snowflakeCortexClient;
    private final SnowflakeProperties snowflakeProperties;

    public SlackEventService(
            QuestionWorkflowService questionWorkflowService,
            KnowledgePageRepository knowledgePageRepository,
            SlackApiClient slackApiClient,
            @Qualifier("slackEventExecutor") TaskExecutor taskExecutor,
            SlackEventDeduplicator deduplicator,
            @Nullable SnowflakeCortexClient snowflakeCortexClient,
            SnowflakeProperties snowflakeProperties
    ) {
        this.questionWorkflowService = questionWorkflowService;
        this.knowledgePageRepository = knowledgePageRepository;
        this.slackApiClient = slackApiClient;
        this.taskExecutor = taskExecutor;
        this.deduplicator = deduplicator;
        this.snowflakeCortexClient = snowflakeCortexClient;
        this.snowflakeProperties = snowflakeProperties;
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
                    "질문을 입력해 주세요. 봇 멘션 뒤에 궁금한 내용을 적어 주세요.");
            return;
        }

        if (snowflakeProperties.isEnabled() && snowflakeCortexClient != null) {
            processWithSnowflake(channel, threadTs, question, eventId);
        } else {
            processWithGemini(channel, threadTs, user, question, eventId);
        }
    }

    private void processWithSnowflake(String channel, String threadTs, String question, String eventId) {
        try {
            SnowflakeCortexResponse response = snowflakeCortexClient.search(question);
            if (snowflakeCortexClient.isRelevant(response)) {
                slackApiClient.postThreadReply(channel, threadTs, buildSnowflakeReply(response));
            } else {
                slackApiClient.postThreadReply(channel, threadTs,
                        "등록되어 있지 않은 질문입니다. 다른 키워드로 다시 질문해 주세요.");
            }
        } catch (Exception exception) {
            log.error("Snowflake Cortex search failed for event {}", eventId, exception);
            safePostFailure(channel, threadTs);
        }
    }

    private void processWithGemini(String channel, String threadTs, String user, String question, String eventId) {
        try {
            QuestionThread thread = questionWorkflowService.submitQuestion(
                    user, channel, question, KnowledgeSourceType.SLACK_THREAD
            );
            KnowledgePage page = knowledgePageRepository.findById(thread.getLinkedPageId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Linked knowledge page was not found: " + thread.getLinkedPageId()));
            slackApiClient.postThreadReply(channel, threadTs, buildGeminiReply(page, thread));
        } catch (Exception exception) {
            log.error("Failed to process Slack event {}", eventId, exception);
            safePostFailure(channel, threadTs);
        }
    }

    private void safePostFailure(String channel, String threadTs) {
        try {
            slackApiClient.postThreadReply(channel, threadTs,
                    "답변 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
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

    private static String buildSnowflakeReply(SnowflakeCortexResponse response) {
        return "*" + response.title() + "*\n\n"
                + response.answer();
    }

    private static String buildGeminiReply(KnowledgePage page, QuestionThread thread) {
        return "*" + page.getTitle() + "*\n"
                + thread.getAiAnswer() + "\n\n"
                + "Saved in Mega-Wiki with page id `" + page.getId() + "`.";
    }
}
