package com.megawiki.service;

import com.megawiki.domain.KnowledgeSourceType;

public interface AiAnswerGenerator {

    AiAnswerDraft generate(String question, KnowledgeSourceType sourceType);
}