package com.megawiki.integration.slack;

import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.service.QuestionSubmissionCommand;
import com.megawiki.service.QuestionSubmissionResult;
import com.megawiki.service.QuestionWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class GeminiSlackMentionProcessor implements SlackMentionProcessor {

    private static final Logger log = LoggerFactory.getLogger(GeminiSlackMentionProcessor.class);

    private final QuestionWorkflowService questionWorkflowService;
    private final SlackApiClient slackApiClient;

    public GeminiSlackMentionProcessor(QuestionWorkflowService questionWorkflowService, SlackApiClient slackApiClient) {
        this.questionWorkflowService = questionWorkflowService;
        this.slackApiClient = slackApiClient;
    }

    @Override
    public boolean supports(SlackMentionRequest request) {
        return true;
    }

    @Override
    public void process(SlackMentionRequest request) {
        try {
            QuestionSubmissionResult result = questionWorkflowService.submit(new QuestionSubmissionCommand(
                    request.user(),
                    request.channel(),
                    request.question(),
                    KnowledgeSourceType.SLACK_THREAD
            ));
            slackApiClient.postThreadReply(
                    request.channel(),
                    request.threadTs(),
                    SlackReplyFormatter.geminiReply(result.page(), result.thread(), result.reusedExistingPage())
            );
        } catch (Exception exception) {
            log.error("Failed to process Slack event {}", request.eventId(), exception);
            safePostFailure(request);
        }
    }

    private void safePostFailure(SlackMentionRequest request) {
        try {
            slackApiClient.postThreadReply(request.channel(), request.threadTs(), SlackReplyFormatter.failure());
        } catch (Exception secondaryException) {
            log.error("Failed to publish Slack failure notice", secondaryException);
        }
    }
}