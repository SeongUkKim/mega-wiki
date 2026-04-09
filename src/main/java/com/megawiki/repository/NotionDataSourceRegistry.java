package com.megawiki.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "notion")
public class NotionDataSourceRegistry {

    private String pagesDataSourceId;
    private String questionsDataSourceId;

    public synchronized void register(String pagesDataSourceId, String questionsDataSourceId) {
        this.pagesDataSourceId = requireText(pagesDataSourceId, "pagesDataSourceId");
        this.questionsDataSourceId = requireText(questionsDataSourceId, "questionsDataSourceId");
    }

    public String getPagesDataSourceId() {
        return requireText(pagesDataSourceId, "pagesDataSourceId");
    }

    public String getQuestionsDataSourceId() {
        return requireText(questionsDataSourceId, "questionsDataSourceId");
    }

    private static String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Notion data source registry is not initialized: " + field);
        }
        return value.trim();
    }
}
