package com.megawiki.service;

import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.QuestionThread;

public record QuestionSubmissionResult(
        KnowledgePage page,
        QuestionThread thread
) {
}