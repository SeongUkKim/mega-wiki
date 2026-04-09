package com.megawiki.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;

public final class Contribution {

    private final String author;
    private final ContributionType type;
    private final String content;
    private final LocalDateTime createdAt;

    @JsonCreator
    public Contribution(
            @JsonProperty("author") String author,
            @JsonProperty("type") ContributionType type,
            @JsonProperty("content") String content,
            @JsonProperty("createdAt") LocalDateTime createdAt
    ) {
        this.author = requireText(author, "author");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.content = requireText(content, "content");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Contribution now(String author, ContributionType type, String content) {
        return new Contribution(author, type, content, LocalDateTime.now());
    }

    public String getAuthor() {
        return author;
    }

    public ContributionType getType() {
        return type;
    }

    public String getContent() {
        return content;
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