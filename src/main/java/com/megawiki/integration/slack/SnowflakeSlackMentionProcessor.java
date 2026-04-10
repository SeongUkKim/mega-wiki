package com.megawiki.integration.slack;

import com.megawiki.integration.snowflake.SnowflakeCortexClient;
import com.megawiki.integration.snowflake.SnowflakeCortexResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@ConditionalOnProperty(name = "mega-wiki.snowflake.enabled", havingValue = "true")
public class SnowflakeSlackMentionProcessor implements SlackMentionProcessor {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeSlackMentionProcessor.class);

    private final SnowflakeCortexClient snowflakeCortexClient;
    private final SlackApiClient slackApiClient;

    public SnowflakeSlackMentionProcessor(SnowflakeCortexClient snowflakeCortexClient, SlackApiClient slackApiClient) {
        this.snowflakeCortexClient = snowflakeCortexClient;
        this.slackApiClient = slackApiClient;
    }

    @Override
    public boolean supports(SlackMentionRequest request) {
        return true;
    }

    @Override
    public void process(SlackMentionRequest request) {
        try {
            SnowflakeCortexResponse response = snowflakeCortexClient.search(request.question());
            if (snowflakeCortexClient.isRelevant(response)) {
                slackApiClient.postThreadReply(
                        request.channel(),
                        request.threadTs(),
                        SlackReplyFormatter.snowflakeReply(response)
                );
            } else {
                slackApiClient.postThreadReply(
                        request.channel(),
                        request.threadTs(),
                        SlackReplyFormatter.knowledgeNotFound()
                );
            }
        } catch (Exception exception) {
            log.error("Snowflake Cortex search failed for event {}", request.eventId(), exception);
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