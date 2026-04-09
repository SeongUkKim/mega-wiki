package com.megawiki.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class QuestionThread {

    private String id;
    private final String author;
    private final String channel;
    private final String question;
    private final String aiAnswer;
    private final String linkedPageId;
    private final KnowledgeSourceType sourceType;
    private final LocalDateTime createdAt;

    private QuestionThread(
            String id,
            String author,
            String channel,
            String question,
            String aiAnswer,
            String linkedPageId,
            KnowledgeSourceType sourceType,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.author = requireText(author, "author");
        this.channel = requireText(channel, "channel");
        this.question = requireText(question, "question");
        this.aiAnswer = requireText(aiAnswer, "aiAnswer");
        this.linkedPageId = requireText(linkedPageId, "linkedPageId");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static QuestionThread create(
            String author,
            String channel,
            String question,
            String aiAnswer,
            String linkedPageId,
            KnowledgeSourceType sourceType
    ) {
        return new QuestionThread(null, author, channel, question, aiAnswer, linkedPageId, sourceType, LocalDateTime.now());
    }

    public static QuestionThread restore(
            String id,
            String author,
            String channel,
            String question,
            String aiAnswer,
            String linkedPageId,
            KnowledgeSourceType sourceType,
            LocalDateTime createdAt
    ) {
        return new QuestionThread(id, author, channel, question, aiAnswer, linkedPageId, sourceType, createdAt);
    }

    public void assignId(String id) {
        if (this.id == null) {
            this.id = requireText(id, "id");
        }
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getChannel() {
        return channel;
    }

    public String getQuestion() {
        return question;
    }

    public String getAiAnswer() {
        return aiAnswer;
    }

    public String getLinkedPageId() {
        return linkedPageId;
    }

    public KnowledgeSourceType getSourceType() {
        return sourceType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}