package com.megawiki.integration.slack;

public interface SlackMentionProcessor {

    boolean supports(SlackMentionRequest request);

    void process(SlackMentionRequest request);
}