package com.megawiki.integration.slack;

public record SlackMentionRequest(
        String eventId,
        String channel,
        String threadTs,
        String user,
        String question
) {
}