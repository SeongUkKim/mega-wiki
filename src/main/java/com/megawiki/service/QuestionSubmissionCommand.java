package com.megawiki.service;

import com.megawiki.domain.KnowledgeSourceType;

public record QuestionSubmissionCommand(
        String author,
        String channel,
        String question,
        KnowledgeSourceType sourceType
) {
}