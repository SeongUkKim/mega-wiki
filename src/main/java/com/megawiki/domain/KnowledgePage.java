package com.megawiki.domain;

import com.megawiki.service.AiAnswerDraft;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class KnowledgePage {

    private String id;
    private final String title;
    private final String slug;
    private String summary;
    private String content;
    private final Set<String> tags;
    private final KnowledgeSourceType sourceType;
    private KnowledgeStatus status;
    private final List<Contribution> contributions;
    private int linkedQuestionCount;
    private int helpfulCount;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private KnowledgePage(
            String id,
            String title,
            String slug,
            String summary,
            String content,
            Set<String> tags,
            KnowledgeSourceType sourceType,
            KnowledgeStatus status,
            List<Contribution> contributions,
            int linkedQuestionCount,
            int helpfulCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.title = requireText(title, "title");
        this.slug = requireText(slug, "slug");
        this.summary = summary == null ? "" : summary.trim();
        this.content = content == null ? "" : content.trim();
        this.tags = normalizeTags(tags);
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.contributions = new ArrayList<>(Objects.requireNonNull(contributions, "contributions must not be null"));
        this.linkedQuestionCount = linkedQuestionCount;
        this.helpfulCount = helpfulCount;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static KnowledgePage create(
            String title,
            String slug,
            String summary,
            String content,
            Collection<String> tags,
            KnowledgeSourceType sourceType,
            KnowledgeStatus status
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new KnowledgePage(
                null,
                title,
                slug,
                summary,
                content,
                new LinkedHashSet<>(tags),
                sourceType,
                status,
                new ArrayList<>(),
                0,
                0,
                now,
                now
        );
    }

    public static KnowledgePage restore(
            String id,
            String title,
            String slug,
            String summary,
            String content,
            Collection<String> tags,
            KnowledgeSourceType sourceType,
            KnowledgeStatus status,
            List<Contribution> contributions,
            int linkedQuestionCount,
            int helpfulCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new KnowledgePage(
                id,
                title,
                slug,
                summary,
                content,
                new LinkedHashSet<>(tags),
                sourceType,
                status,
                contributions,
                linkedQuestionCount,
                helpfulCount,
                createdAt,
                updatedAt
        );
    }

    public void assignId(String id) {
        if (this.id == null) {
            this.id = requireText(id, "id");
        }
    }

    public void mergeAiDraft(AiAnswerDraft draft, String originalQuestion) {
        Objects.requireNonNull(draft, "draft must not be null");
        summary = draft.summary();
        if (content.isBlank()) {
            content = draft.content();
        } else if (!content.contains(draft.content())) {
            content = content + "\n\n---\n\n" + draft.content();
        }
        tags.addAll(draft.tags());
        linkedQuestionCount += 1;
        addContribution("Mega-Wiki AI", ContributionType.AI_ANSWER,
                draft.answer() + "\n\nOriginal question: " + originalQuestion);
        status = KnowledgeStatus.DRAFT;
    }

    public void addContribution(String author, ContributionType type, String content) {
        contributions.add(0, Contribution.now(author, type, content));
        if (type != ContributionType.AI_ANSWER && status == KnowledgeStatus.DRAFT) {
            status = KnowledgeStatus.CURATED;
        }
        updatedAt = LocalDateTime.now();
    }

    public void markHelpful() {
        helpfulCount += 1;
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public KnowledgeSourceType getSourceType() {
        return sourceType;
    }

    public KnowledgeStatus getStatus() {
        return status;
    }

    public List<Contribution> getContributions() {
        return List.copyOf(contributions);
    }

    public int getLinkedQuestionCount() {
        return linkedQuestionCount;
    }

    public int getHelpfulCount() {
        return helpfulCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private static Set<String> normalizeTags(Collection<String> rawTags) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : rawTags) {
            if (tag != null && !tag.isBlank()) {
                normalized.add(tag.trim());
            }
        }
        return normalized;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}