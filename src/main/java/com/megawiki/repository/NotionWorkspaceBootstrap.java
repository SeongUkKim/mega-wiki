package com.megawiki.repository;

import com.megawiki.config.NotionProperties;
import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "notion")
public class NotionWorkspaceBootstrap {

    private static final Logger log = LoggerFactory.getLogger(NotionWorkspaceBootstrap.class);

    private final NotionApiClient notionApiClient;
    private final NotionProperties notionProperties;
    private final NotionDataSourceRegistry registry;

    public NotionWorkspaceBootstrap(
            NotionApiClient notionApiClient,
            NotionProperties notionProperties,
            NotionDataSourceRegistry registry
    ) {
        this.notionApiClient = notionApiClient;
        this.notionProperties = notionProperties;
        this.registry = registry;
    }

    @PostConstruct
    public void initialize() {
        String pagesDataSourceId = resolveDataSourceId(
                notionProperties.getPagesDataSourceId(),
                notionProperties.getPagesDataSourceName()
        );
        String questionsDataSourceId = resolveDataSourceId(
                notionProperties.getQuestionsDataSourceId(),
                notionProperties.getQuestionsDataSourceName()
        );

        registry.register(pagesDataSourceId, questionsDataSourceId);
        notionApiClient.updateDataSource(pagesDataSourceId, Map.of("properties", buildPagesSchema()));
        notionApiClient.updateDataSource(questionsDataSourceId, Map.of("properties", buildQuestionsSchema()));

        log.info("Resolved Notion data sources. pages={}, questions={}", pagesDataSourceId, questionsDataSourceId);
    }

    private String resolveDataSourceId(String configuredId, String expectedName) {
        if (StringUtils.hasText(configuredId)) {
            return configuredId.trim();
        }

        List<NotionApiClient.DataSourceSummary> exactMatches = notionApiClient.searchDataSources(expectedName).stream()
                .filter(summary -> expectedName.equalsIgnoreCase(summary.title()))
                .toList();

        if (exactMatches.isEmpty()) {
            throw new IllegalStateException(
                    "Notion data source not found: " + expectedName
                            + ". Create the database, connect the integration, or set the explicit data source id."
            );
        }

        if (exactMatches.size() > 1) {
            log.warn("Multiple Notion data sources matched '{}'. The first match will be used.", expectedName);
        }

        return exactMatches.get(0).id();
    }

    private static Map<String, Object> buildPagesSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("title", Map.of("name", "Name"));
        properties.put("Slug", richTextProperty());
        properties.put("Summary", richTextProperty());
        properties.put("Content", richTextProperty());
        properties.put("ContributionsJson", richTextProperty());
        properties.put("SourceType", selectProperty(List.of(
                option("SLACK_THREAD", "blue"),
                option("MEGAONE_ASK", "green"),
                option("MANUAL", "gray")
        )));
        properties.put("Status", selectProperty(List.of(
                option("DRAFT", "yellow"),
                option("CURATED", "blue"),
                option("VERIFIED", "green")
        )));
        properties.put("Tags", multiSelectProperty(List.of(
                option("onboarding", "blue"),
                option("hr", "green"),
                option("remote-work", "yellow"),
                option("faq", "purple"),
                option("ask", "orange"),
                option("knowledge-share", "pink"),
                option("knowledge-management", "purple"),
                option("slack", "red"),
                option("operations", "brown"),
                option("team-knowledge", "gray"),
                option("manual", "default"),
                option("wiki", "blue")
        )));
        properties.put("LinkedQuestions", numberProperty());
        properties.put("HelpfulCount", numberProperty());
        return properties;
    }

    private static Map<String, Object> buildQuestionsSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("title", Map.of("name", "Name"));
        properties.put("Author", richTextProperty());
        properties.put("Channel", richTextProperty());
        properties.put("Question", richTextProperty());
        properties.put("AiAnswer", richTextProperty());
        properties.put("LinkedPageId", richTextProperty());
        properties.put("SourceType", selectProperty(List.of(
                option("SLACK_THREAD", "blue"),
                option("MEGAONE_ASK", "green"),
                option("MANUAL", "gray")
        )));
        return properties;
    }

    private static Map<String, Object> richTextProperty() {
        return Map.of("rich_text", Map.of());
    }

    private static Map<String, Object> numberProperty() {
        return Map.of("number", Map.of("format", "number"));
    }

    private static Map<String, Object> selectProperty(List<Map<String, Object>> options) {
        return Map.of("select", Map.of("options", options));
    }

    private static Map<String, Object> multiSelectProperty(List<Map<String, Object>> options) {
        return Map.of("multi_select", Map.of("options", options));
    }

    private static Map<String, Object> option(String name, String color) {
        return Map.of(
                "name", name,
                "color", color
        );
    }
}
