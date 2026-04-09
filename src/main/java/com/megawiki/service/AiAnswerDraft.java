package com.megawiki.service;

import java.util.Set;

public record AiAnswerDraft(
        String title,
        String summary,
        String answer,
        String content,
        Set<String> tags
) {
}